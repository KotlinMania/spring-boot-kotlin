/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.build.bom.bomr

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Transformer
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.options.Option
import org.springframework.boot.build.bom.BomExtension
import org.springframework.boot.build.bom.Library
import org.springframework.boot.build.bom.Library.ProhibitedVersion
import org.springframework.boot.build.bom.UpgradePolicy.Companion.max
import org.springframework.boot.build.bom.bomr.VersionOption.ResolvedVersionOption
import org.springframework.boot.build.bom.bomr.github.GitHub
import org.springframework.boot.build.bom.bomr.github.GitHubRepository
import org.springframework.boot.build.bom.bomr.github.Issue
import org.springframework.boot.build.bom.bomr.github.Milestone
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import org.springframework.util.StringUtils
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*
import java.util.function.BiFunction
import java.util.function.BiPredicate
import java.util.regex.Pattern
import javax.inject.Inject
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty

/**
 * Base class for tasks that upgrade dependencies in a BOM.
 * 
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
abstract class UpgradeDependencies protected constructor(private val bom: BomExtension, movingToSnapshots: Boolean) :
    DefaultTask() {
    private val movingToSnapshots: Boolean

    private val upgradeApplicator: UpgradeApplicator

    private val repositories: RepositoryHandler

    @Inject
    constructor(bom: BomExtension) : this(bom, false)

    init {
        this.threads.convention(2)
        this.movingToSnapshots = movingToSnapshots
        this.upgradeApplicator = UpgradeApplicator(
            project.getBuildFile().toPath(),
            File(project.getRootProject().projectDir, "gradle.properties").toPath()
        )
        this.repositories = project.getRepositories()
    }

    @get:Option(
        option = "milestone",
        description = "Milestone to which dependency upgrade issues should be assigned"
    )
    @get:Input
    abstract val milestone: Property<String>

    @get:Option(
        option = "threads",
        description = "Number of Threads to use for update resolution"
    )
    @get:Optional
    @get:Input
    abstract val threads: Property<Int>

    @get:Option(
        option = "libraries",
        description = "Regular expression that identifies the libraries to upgrade"
    )
    @get:Optional
    @get:Input
    abstract val libraries: Property<String>

    @get:Option(
        option = "dry-run-upgrades",
        description = "Whether to perform a dry run that doesn't open issues or change the bom"
    )
    @get:Optional
    @get:Input
    abstract val dryRun: Property<Boolean>

    @get:Input
    abstract val repositoryNames: ListProperty<String>

    @TaskAction
    open fun upgradeDependencies() {
        val repository = createGitHub().getRepository(
            this.bom.upgrade.gitHub!!.organization,
            this.bom.upgrade.gitHub!!.repository
        )
        val issueLabels = verifyLabels(repository)
        val milestone = determineMilestone(repository)
        val upgrades = resolveUpgrades(milestone)
        if (!this.dryRun.getOrElse(false)) {
            applyUpgrades(repository, issueLabels, milestone, upgrades)
        }
        upgradesApplied(upgrades)
    }

    protected open fun upgradesApplied(upgrades: MutableList<Upgrade>?) {
    }

    private fun applyUpgrades(
        repository: GitHubRepository, issueLabels: MutableList<String?>?, milestone: Milestone?,
        upgrades: MutableList<Upgrade>
    ) {
        val existingUpgradeIssues = repository.findIssues(issueLabels, milestone)
        println("Applying upgrades...")
        println("")
        for (upgrade in upgrades) {
            println(upgrade.to.nameAndVersion)
            val existingUpgradeIssue = findExistingUpgradeIssue(existingUpgradeIssues, upgrade)
            try {
                val modified = this.upgradeApplicator.apply(upgrade)
                val title = issueTitle(upgrade)
                val body = issueBody(upgrade, existingUpgradeIssue)
                val issueNumber = getOrOpenUpgradeIssue(
                    repository, issueLabels, milestone, title, body,
                    existingUpgradeIssue
                )
                if (existingUpgradeIssue != null && existingUpgradeIssue.state == Issue.State.CLOSED) {
                    existingUpgradeIssue.label(mutableListOf<String?>("type: task", "status: superseded"))
                }
                println(
                    ("   Issue: " + issueNumber + " - " + title
                            + getExistingUpgradeIssueMessageDetails(existingUpgradeIssue))
                )
                check(
                    ProcessBuilder().command("git", "add", modified.toFile().absolutePath)
                        .start()
                        .waitFor() == 0
                ) { "git add failed" }
                val commitMessage = commitMessage(upgrade, issueNumber)
                check(
                    ProcessBuilder().command("git", "commit", "-m", commitMessage).start().waitFor() == 0
                ) { "git commit failed" }
                println("  Commit: " + commitMessage.substring(0, commitMessage.indexOf('\n')))
            } catch (ex: IOException) {
                throw TaskExecutionException(this, ex)
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun getOrOpenUpgradeIssue(
        repository: GitHubRepository, issueLabels: MutableList<String?>?, milestone: Milestone?,
        title: String?, body: String?, existingUpgradeIssue: Issue?
    ): Int {
        if (existingUpgradeIssue != null && existingUpgradeIssue.state == Issue.State.OPEN) {
            return existingUpgradeIssue.number
        }
        return repository.openIssue(title, body, issueLabels, milestone)
    }

    private fun getExistingUpgradeIssueMessageDetails(existingUpgradeIssue: Issue?): String {
        if (existingUpgradeIssue == null) {
            return ""
        }
        if (existingUpgradeIssue.state != Issue.State.CLOSED) {
            return " (completes existing upgrade)"
        }
        return " (supersedes #" + existingUpgradeIssue.number + " " + existingUpgradeIssue.title + ")"
    }

    private fun verifyLabels(repository: GitHubRepository): MutableList<String?> {
        val availableLabels = repository.labels
        val issueLabels = this.bom.upgrade.gitHub!!.issueLabels
        if (!availableLabels.containsAll(issueLabels!!)) {
            val unknownLabels: MutableList<String?> = ArrayList<String?>(issueLabels)
            unknownLabels.removeAll(availableLabels)
            val suffix = if (unknownLabels.size == 1) "" else "s"
            throw InvalidUserDataException(
                "Unknown label" + suffix + ": " + StringUtils.collectionToCommaDelimitedString(unknownLabels)
            )
        }
        return issueLabels
    }

    private fun createGitHub(): GitHub {
        val bomrProperties = Properties()
        try {
            FileReader(File(System.getProperty("user.home"), ".bomr.properties")).use { reader ->
                bomrProperties.load(reader)
                val username = bomrProperties.getProperty("bomr.github.username")
                val password = bomrProperties.getProperty("bomr.github.password")
                return GitHub.withCredentials(username, password)
            }
        } catch (ex: IOException) {
            throw InvalidUserDataException("Failed to load .bomr.properties from user home", ex)
        }
    }

    private fun determineMilestone(repository: GitHubRepository): Milestone {
        val milestones = repository.milestones
        val matchingMilestone: java.util.Optional<Milestone> = milestones.stream()
            .filter { milestone: Milestone? -> milestone!!.name == this.milestone.get() }
            .findFirst()
        if (matchingMilestone.isEmpty()) {
            throw InvalidUserDataException("Unknown milestone: " + this.milestone.get())
        }
        return matchingMilestone.get()
    }

    private fun findExistingUpgradeIssue(existingUpgradeIssues: MutableList<Issue>, upgrade: Upgrade): Issue? {
        val toMatch = "Upgrade to " + upgrade.toRelease.name
        for (existingUpgradeIssue in existingUpgradeIssues) {
            var title = existingUpgradeIssue.title
            val lastSpaceIndex = title.lastIndexOf(' ')
            if (lastSpaceIndex > -1) {
                title = title.substring(0, lastSpaceIndex)
            }
            if (title == toMatch) {
                return existingUpgradeIssue
            }
        }
        return null
    }

    @Suppress("deprecation")
    private fun resolveUpgrades(milestone: Milestone?): MutableList<Upgrade> {
        val upgradeResolver = InteractiveUpgradeResolver(
            getServices().get<UserInputHandler?>(UserInputHandler::class.java), getLibraryUpdateResolver(milestone)
        )
        return upgradeResolver.resolveUpgrades(matchingLibraries(), this.bom.libraries)
    }

    private fun getLibraryUpdateResolver(milestone: Milestone?): LibraryUpdateResolver {
        val versionResolver: VersionResolver = MavenMetadataVersionResolver(getRepositories())
        val libraryResolver: LibraryUpdateResolver = StandardLibraryUpdateResolver(
            versionResolver,
            createVersionOptionResolver(milestone)
        )
        return MultithreadedLibraryUpdateResolver(this.threads.get(), libraryResolver)
    }

    private fun getRepositories(): MutableCollection<MavenArtifactRepository?> {
        return this.repositoryNames.map<MutableList<MavenArtifactRepository?>>(Transformer { repositoryNames: MutableList<String?>? ->
            this.asRepositories(
                repositoryNames!!
            )
        }).get()
    }

    private fun asRepositories(repositoryNames: MutableList<String?>): MutableList<MavenArtifactRepository?> {
        return repositoryNames.stream()
            .map<ArtifactRepository> { name: String? -> this.repositories.getByName(name!!) }
            .map<MavenArtifactRepository> { obj: ArtifactRepository? -> MavenArtifactRepository::class.java.cast(obj) }
            .toList()
    }

    protected open fun createVersionOptionResolver(milestone: Milestone?): BiFunction<Library?, DependencyVersion?, VersionOption?>? {
        val updatePredicates: MutableList<BiPredicate<Library?, DependencyVersion?>?> =
            ArrayList<BiPredicate<Library?, DependencyVersion?>?>()
        updatePredicates.add(BiPredicate { library: Library?, candidate: DependencyVersion? ->
            this.compliesWithUpgradePolicy(
                library!!,
                candidate
            )
        })
        updatePredicates.add(BiPredicate { library: Library?, candidate: DependencyVersion? ->
            this.isAnUpgrade(
                library!!,
                candidate
            )
        })
        updatePredicates.add(BiPredicate { library: Library?, candidate: DependencyVersion? ->
            this.isNotProhibited(
                library!!,
                candidate!!
            )
        })
        return BiFunction { library: Library?, dependencyVersion: DependencyVersion? ->
            if (this.compliesWithUpgradePolicy(library!!, dependencyVersion)
                && this.isAnUpgrade(library, dependencyVersion)
                && this.isNotProhibited(library, dependencyVersion!!)
            ) {
                return@BiFunction ResolvedVersionOption(dependencyVersion, mutableListOf<String?>())
            }
            null
        }
    }

    private fun compliesWithUpgradePolicy(library: Library, candidate: DependencyVersion?): Boolean {
        val libraryPolicy = library.upgradePolicy
        val bomPolicy = this.bom.upgrade.policy
        val upgradePolicy = max(libraryPolicy, bomPolicy)
        return upgradePolicy.test(candidate, library.version!!.version)
    }

    private fun isAnUpgrade(library: Library, candidate: DependencyVersion?): Boolean {
        return library.version!!.version.isUpgrade(candidate, this.movingToSnapshots)
    }

    private fun isNotProhibited(library: Library, candidate: DependencyVersion): Boolean {
        return library.prohibitedVersions!!
            .stream()
            .noneMatch { prohibited: ProhibitedVersion? -> prohibited!!.isProhibited(candidate.toString()) }
    }

    private fun matchingLibraries(): MutableList<Library?> {
        val matchingLibraries =
            this.bom.libraries.stream().filter { library: Library? -> this.eligible(library!!) }.toList()
        if (matchingLibraries.isEmpty()) {
            throw InvalidUserDataException("No libraries to upgrade")
        }
        return matchingLibraries
    }

    protected open fun eligible(library: Library): Boolean {
        val pattern = this.libraries.getOrNull()
        if (pattern == null) {
            return true
        }
        val libraryPredicate = Pattern.compile(pattern).asPredicate()
        return libraryPredicate.test(library.name)
    }

    protected abstract fun commitMessage(upgrade: Upgrade?, issueNumber: Int): String

    protected fun issueTitle(upgrade: Upgrade): String {
        return "Upgrade to " + upgrade.toRelease.nameAndVersion
    }

    protected fun issueBody(upgrade: Upgrade, existingUpgrade: Issue?): String {
        val description = upgrade.toRelease.nameAndVersion
        val releaseNotesLink = upgrade.toRelease.getLinkUrl("releaseNotes")
        var body: String = if (releaseNotesLink != null)
            "Upgrade to [%s](%s).".format(description, releaseNotesLink)
        else
            "Upgrade to %s.".format(description)
        if (existingUpgrade != null) {
            body += "\n\nSupersedes #" + existingUpgrade.number
        }
        return body
    }
}
