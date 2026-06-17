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

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.build.bom.BomExtension
import org.springframework.boot.build.bom.Library
import org.springframework.boot.build.bom.bomr.VersionOption.SnapshotVersionOption
import org.springframework.boot.build.bom.bomr.github.Milestone
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import org.springframework.boot.build.properties.BuildProperties
import org.springframework.boot.build.properties.BuildType
import java.time.OffsetDateTime
import java.util.function.BiFunction
import javax.inject.Inject

/**
 * A [Task] to move to snapshot dependencies.
 * 
 * @author Andy Wilkinson
 */
abstract class MoveToSnapshots @Inject constructor(bom: BomExtension?) : UpgradeDependencies(bom, true) {
    private val buildType: BuildType? = BuildProperties.get(getProject()).buildType

    init {
        getProject().getRepositories().withType<MavenArtifactRepository>(
            MavenArtifactRepository::class.java) { repository: MavenArtifactRepository ->
                val name = repository!!.name
                if (name.startsWith("spring-") && name.endsWith("-snapshot")) {
                    getRepositoryNames().add(name)
                }
            }
    }

    @TaskAction
    override fun upgradeDependencies() {
        super.upgradeDependencies()
    }

    override fun commitMessage(upgrade: Upgrade, issueNumber: Int): String {
        return ("Start building against " + upgrade.toRelease.nameAndVersion + " snapshots" + "\n\nSee gh-"
                + issueNumber)
    }

    override fun eligible(library: Library): Boolean {
        return library.isConsiderSnapshots && super.eligible(library)
    }

    override fun createVersionOptionResolver(milestone: Milestone): BiFunction<Library?, DependencyVersion?, VersionOption?>? {
        return when (this.buildType) {
            BuildType.OPEN_SOURCE -> createOpenSourceVersionOptionResolver(milestone)
            BuildType.COMMERCIAL -> super.createVersionOptionResolver(milestone)
        }
    }

    private fun createOpenSourceVersionOptionResolver(
        milestone: Milestone
    ): BiFunction<Library?, DependencyVersion?, VersionOption?> {
        val scheduledReleases = getScheduledOpenSourceReleases(milestone)
        val resolver = super.createVersionOptionResolver(milestone)
        return BiFunction { library: Library?, dependencyVersion: DependencyVersion? ->
            val versionOption = resolver.apply(library, dependencyVersion)
            if (versionOption != null) {
                val releases = scheduledReleases.get(library!!.calendarName)
                if (releases != null) {
                    val matches = releases.stream()
                        .filter { release: ReleaseSchedule.Release? -> dependencyVersion!!.isSnapshotFor(release!!.version) }
                        .toList()
                    if (!matches.isEmpty()) {
                        return@BiFunction SnapshotVersionOption(
                            versionOption.version,
                            matches.get(0)!!.version
                        )
                    }
                }
                if (Companion.logger.isInfoEnabled()) {
                    Companion.logger.info(
                        "Ignoring {}. No release of {} scheduled before {}", dependencyVersion,
                        library.name, milestone.dueOn
                    )
                }
            }
            null
        }
    }

    private fun getScheduledOpenSourceReleases(milestone: Milestone): MutableMap<String?, MutableList<ReleaseSchedule.Release?>?> {
        val releaseSchedule = ReleaseSchedule()
        return releaseSchedule.releasesBetween(OffsetDateTime.now(), milestone.dueOn)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MoveToSnapshots::class.java)
    }
}
