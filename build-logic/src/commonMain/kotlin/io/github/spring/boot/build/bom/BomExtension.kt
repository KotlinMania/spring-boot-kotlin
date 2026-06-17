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
package org.springframework.boot.build.bom

import groovy.lang.Closure
import groovy.lang.GroovyObjectSupport
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException
import org.apache.maven.artifact.versioning.VersionRange
import org.gradle.api.Action
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPlatformPlugin
import org.springframework.boot.build.bom.Library.*
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import org.springframework.boot.build.properties.BuildProperties
import org.springframework.util.PropertyPlaceholderHelper
import java.util.List
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import javax.inject.Inject

/**
 * DSL extensions for [BomPlugin].
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class BomExtension(val project: Project) {
    val id: String

    val upgradeHandler: UpgradeHandler

    val properties: MutableMap<String?, DependencyVersion?> = LinkedHashMap<String?, DependencyVersion?>()

    val artifactVersionProperties: MutableMap<String?, String?> = HashMap<String?, String?>()

    val libraries: MutableList<Library> = ArrayList<Library>()

    init {
        this.upgradeHandler = project.getObjects().newInstance<UpgradeHandler>(UpgradeHandler::class.java, project)
        this.id = "%s:%s:%s".format(project.getGroup(), project.name, project.version)
    }

    fun getLibrary(name: String?): Library {
        return this.libraries.stream()
            .filter { library: Library? -> library!!.name == name }
            .findFirst()
            .orElseThrow<IllegalStateException?>(Supplier {
                IllegalStateException(
                    "No library found named '%s'".format(
                        name
                    )
                )
            })
    }

    fun upgrade(action: Action<UpgradeHandler?>) {
        action.execute(this.upgradeHandler)
    }

    val upgrade: Upgrade
        get() {
            val gitHub = this.upgradeHandler.gitHub
            return BomExtension.Upgrade(
                this.upgradeHandler.upgradePolicy,
                BomExtension.GitHub(
                    gitHub.organization,
                    gitHub.repository,
                    gitHub.issueLabels
                )
            )
        }

    fun library(name: String?, action: Action<LibraryHandler?>) {
        library(name, null, action)
    }

    fun library(name: String?, version: String?, action: Action<LibraryHandler?>) {
        val objects = this.project.getObjects()
        val libraryHandler = objects.newInstance<LibraryHandler>(
            LibraryHandler::class.java, this.project,
            if (version != null) version else ""
        )
        action.execute(libraryHandler)
        val libraryVersion = LibraryVersion(DependencyVersion.parse(libraryHandler.version))
        addLibrary(
            Library(
                name, libraryHandler.calendarName, libraryVersion, libraryHandler.groups,
                libraryHandler.upgradePolicy, libraryHandler.prohibitedVersions, libraryHandler.considerSnapshots,
                versionAlignment(libraryHandler), libraryHandler.alignWith.bomAlignment, libraryHandler.linkRootName,
                libraryHandler.links
            )
        )
    }

    fun versionAlignment(libraryHandler: LibraryHandler): VersionAlignment? {
        val version = libraryHandler.alignWith.version
        if (version != null) {
            return DependencyVersionAlignment(
                version.of, version.from, version.managedBy, this.project,
                this.libraries, libraryHandler.groups
            )
        }
        val property = libraryHandler.alignWith.property
        if (property != null) {
            return PomPropertyVersionAlignment(
                property.name, property.of, property.managedBy, this.project,
                this.libraries
            )
        }
        return null
    }

    fun createDependencyNotation(groupId: String?, artifactId: String?, version: DependencyVersion?): String {
        return groupId + ":" + artifactId + ":" + version
    }

    fun getArtifactVersionProperty(groupId: String?, artifactId: String?, classifier: String?): String? {
        val coordinates = groupId + ":" + artifactId + ":" + classifier
        return this.artifactVersionProperties.get(coordinates)
    }

    fun putArtifactVersionProperty(groupId: String?, artifactId: String?, versionProperty: String?) {
        putArtifactVersionProperty(groupId, artifactId, null, versionProperty)
    }

    fun putArtifactVersionProperty(
        groupId: String?, artifactId: String?, classifier: String?,
        versionProperty: String?
    ) {
        val coordinates = groupId + ":" + artifactId + ":" + (if (classifier != null) classifier else "")
        val existing = this.artifactVersionProperties.putIfAbsent(coordinates, versionProperty)
        if (existing != null) {
            throw InvalidUserDataException(
                ("Cannot put version property for '" + coordinates
                        + "'. Version property '" + existing + "' has already been stored.")
            )
        }
    }

    fun addLibrary(library: Library) {
        val dependencies = this.project.getDependencies()
        this.libraries.add(library)
        val versionProperty = library.versionProperty
        if (versionProperty != null) {
            this.properties.put(versionProperty, library.version.version)
        }
        for (group in library.groups) {
            for (module in group.modules) {
                addModule(library, dependencies, versionProperty, group, module)
            }
            for (bomImport in group.getBoms()) {
                addBomImport(library, dependencies, versionProperty, group, bomImport.name)
            }
        }
    }

    fun addModule(
        library: Library, dependencies: DependencyHandler, versionProperty: String?, group: Library.Group,
        module: Library.Module
    ) {
        putArtifactVersionProperty(group.id, module.name, module.classifier, versionProperty)
        val constraint = createDependencyNotation(
            group.id, module.name,
            library.version.version
        )
        dependencies.getConstraints().add(JavaPlatformPlugin.API_CONFIGURATION_NAME, constraint)
    }

    fun addBomImport(
        library: Library, dependencies: DependencyHandler, versionProperty: String?, group: Library.Group,
        bomImport: String?
    ) {
        putArtifactVersionProperty(group.id, bomImport, versionProperty)
        val bomDependency = createDependencyNotation(group.id, bomImport, library.version.version)
        dependencies.add(JavaPlatformPlugin.API_CONFIGURATION_NAME, dependencies.platform(bomDependency))
        dependencies.add(
            BomPlugin.Companion.API_ENFORCED_CONFIGURATION_NAME,
            dependencies.enforcedPlatform(bomDependency)
        )
    }

    class LibraryHandler @Inject constructor(val project: Project, var version: String?) {
        val groups: MutableList<Library.Group?> = ArrayList<Library.Group?>()

        var upgradePolicy: UpgradePolicy? = null

        val prohibitedVersions: MutableList<ProhibitedVersion?> = ArrayList<ProhibitedVersion?>()

        val alignWith: AlignWithHandler

        var considerSnapshots = false

        var calendarName: String? = null

        var linkRootName: String? = null

        val links: MutableMap<String?, MutableList<Library.Link?>?> =
            HashMap<String?, MutableList<Library.Link?>?>()

        init {
            this.alignWith = project.getObjects().newInstance<AlignWithHandler>(AlignWithHandler::class.java)
        }

        fun version(version: String?) {
            this.version = version
        }

        fun considerSnapshots() {
            this.considerSnapshots = true
        }

        fun setCalendarName(calendarName: String?) {
            this.calendarName = calendarName
        }

        fun group(id: String, action: Action<GroupHandler?>) {
            val groupHandler = this.project.getObjects().newInstance<GroupHandler>(GroupHandler::class.java, id)
            action.execute(groupHandler)
            this.groups
                .add(Library.Group(groupHandler.id, groupHandler.modules, groupHandler.plugins, groupHandler.imports))
        }

        fun setUpgradePolicy(upgradePolicy: UpgradePolicy?) {
            this.upgradePolicy = upgradePolicy
        }

        fun prohibit(action: Action<ProhibitedHandler?>) {
            val handler = ProhibitedHandler()
            action.execute(handler)
            this.prohibitedVersions.add(
                ProhibitedVersion(
                    handler.versionRange, handler.startsWith,
                    handler.endsWith, handler.contains, handler.reason
                )
            )
        }

        fun alignWith(action: Action<AlignWithHandler?>) {
            action.execute(this.alignWith)
        }

        fun links(action: Action<LinksHandler?>) {
            links(null, action)
        }

        fun links(linkRootName: String?, action: Action<LinksHandler?>) {
            val handler = LinksHandler()
            action.execute(handler)
            this.linkRootName = linkRootName
            this.links.putAll(handler.links)
        }

        class ProhibitedHandler {
            var reason: String? = null

            val startsWith: MutableList<String?> = ArrayList<String?>()

            val endsWith: MutableList<String?> = ArrayList<String?>()

            val contains: MutableList<String?> = ArrayList<String?>()

            var versionRange: VersionRange? = null

            fun versionRange(versionRange: String?) {
                try {
                    this.versionRange = VersionRange.createFromVersionSpec(versionRange)
                } catch (ex: InvalidVersionSpecificationException) {
                    throw InvalidUserCodeException("Invalid version range", ex)
                }
            }

            fun startsWith(startsWith: String?) {
                this.startsWith.add(startsWith)
            }

            fun startsWith(startsWith: MutableCollection<String?>) {
                this.startsWith.addAll(startsWith)
            }

            fun endsWith(endsWith: String?) {
                this.endsWith.add(endsWith)
            }

            fun endsWith(endsWith: MutableCollection<String?>) {
                this.endsWith.addAll(endsWith)
            }

            fun contains(contains: String?) {
                this.contains.add(contains)
            }

            fun contains(contains: MutableList<String?>) {
                this.contains.addAll(contains)
            }

            fun because(because: String?) {
                this.reason = because
            }
        }

        class GroupHandler @Inject constructor(val id: String?) : GroovyObjectSupport() {
            var modules: MutableList<Library.Module?> = ArrayList<Library.Module?>()

            val imports: MutableList<ImportedBom?> = ArrayList<ImportedBom?>()

            var plugins: MutableList<String?>? = ArrayList<String?>()

            fun setModules(modules: MutableList<Any?>) {
                this.modules = modules.stream()
                    .map<Library.Module> { input: Any? -> if (input is Library.Module) input else Library.Module(input as String?) }
                    .toList()
            }

            fun bom(bom: String?) {
                this.imports.add(ImportedBom(bom))
            }

            fun bom(bom: String?, action: Action<ImportBomHandler?>) {
                val handler = ImportBomHandler()
                action.execute(handler)
                this.imports.add(ImportedBom(bom, handler.permittedDependencies))
            }

            fun setPlugins(plugins: MutableList<String?>?) {
                this.plugins = plugins
            }

            fun methodMissing(name: String?, args: Any?): Any {
                if (args is Array<Any> && args.size == 1) {
                    if (args[0] is Closure<*>) {
                        val moduleHandler: ModuleHandler = GroupHandler.ModuleHandler()
                        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
                        closure.setDelegate(moduleHandler)
                        closure.call(moduleHandler)
                        return Library.Module(
                            name,
                            moduleHandler.type,
                            moduleHandler.classifier,
                            moduleHandler.exclusions
                        )
                    }
                }
                throw InvalidUserDataException("Invalid configuration for module '" + name + "'")
            }

            inner class ModuleHandler {
                val exclusions: MutableList<Library.Exclusion?> = ArrayList<Library.Exclusion?>()

                var type: String? = null

                var classifier: String? = null

                fun exclude(exclusion: MutableMap<String?, String?>) {
                    this.exclusions.add(Library.Exclusion(exclusion.get("group"), exclusion.get("module")))
                }

                fun setType(type: String?) {
                    this.type = type
                }

                fun setClassifier(classifier: String?) {
                    this.classifier = classifier
                }
            }

            inner class ImportBomHandler {
                val permittedDependencies: MutableList<PermittedDependency?> = ArrayList<PermittedDependency?>()

                fun permit(allowed: String) {
                    val components: Array<String?> =
                        allowed.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    this.permittedDependencies.add(PermittedDependency(components[0], components[1]))
                }
            }
        }

        class AlignWithHandler {
            var version: VersionHandler? = null

            var property: PropertyHandler? = null

            var bomAlignment: BomAlignment? = null

            fun version(action: Action<VersionHandler?>) {
                this.version = VersionHandler()
                action.execute(this.version)
            }

            fun property(action: Action<PropertyHandler?>) {
                this.property = PropertyHandler()
                action.execute(this.property)
            }

            fun dependencyManagementDeclaredIn(bomCoordinates: String?) {
                this.bomAlignment = BomAlignment(bomCoordinates, Predicate { id: ResolvedBom.Id? -> false })
            }

            fun dependencyManagementDeclaredIn(
                bomCoordinates: String?,
                action: Action<DependencyManagementDeclaredInHandler?>
            ) {
                val handler = DependencyManagementDeclaredInHandler()
                action.execute(handler)
                this.bomAlignment = BomAlignment(bomCoordinates, handler.exclusions)
            }

            class VersionHandler {
                var of: String? = null

                var from: String? = null

                var managedBy: String? = null

                fun of(of: String?) {
                    this.of = of
                }

                fun from(from: String?) {
                    this.from = from
                }

                fun managedBy(managedBy: String?) {
                    this.managedBy = managedBy
                }
            }

            class PropertyHandler {
                var name: String? = null

                var of: String? = null

                var managedBy: String? = null

                fun name(name: String?) {
                    this.name = name
                }

                fun of(dependency: String?) {
                    this.of = dependency
                }

                fun managedBy(managedBy: String?) {
                    this.managedBy = managedBy
                }
            }

            class DependencyManagementDeclaredInHandler {
                var exclusions = Predicate { id: ResolvedBom.Id? -> false }

                fun excluding(exclusion: Predicate<ResolvedBom.Id?>) {
                    this.exclusions = this.exclusions.or(exclusion)
                }
            }
        }
    }

    class LinksHandler {
        val links: MutableMap<String?, MutableList<Library.Link?>?> =
            HashMap<String?, MutableList<Library.Link?>?>()

        fun site(linkTemplate: String) {
            site(asFactory(linkTemplate))
        }

        fun site(linkFactory: Function<LibraryVersion?, String?>?) {
            add("site", linkFactory)
        }

        fun github(linkTemplate: String) {
            github(asFactory(linkTemplate))
        }

        fun github(linkFactory: Function<LibraryVersion?, String?>?) {
            add("github", linkFactory)
        }

        fun docs(linkTemplate: String) {
            docs(asFactory(linkTemplate))
        }

        fun docs(linkFactory: Function<LibraryVersion?, String?>?) {
            add("docs", linkFactory)
        }

        fun javadoc(linkTemplate: String) {
            javadoc(asFactory(linkTemplate))
        }

        fun javadoc(linkTemplate: String, vararg packages: String?) {
            javadoc(asFactory(linkTemplate), *packages)
        }

        fun javadoc(linkFactory: Function<LibraryVersion?, String?>?) {
            add("javadoc", linkFactory)
        }

        fun javadoc(linkFactory: Function<LibraryVersion?, String?>?, vararg packages: String?) {
            add("javadoc", linkFactory, packages)
        }

        fun javadoc(rootName: String?, linkFactory: Function<LibraryVersion?, String?>?, vararg packages: String?) {
            add(rootName, "javadoc", linkFactory, packages)
        }

        fun releaseNotes(linkTemplate: String) {
            releaseNotes(asFactory(linkTemplate))
        }

        fun releaseNotes(linkFactory: Function<LibraryVersion?, String?>?) {
            add("releaseNotes", linkFactory)
        }

        fun add(name: String?, linkTemplate: String) {
            add(name, asFactory(linkTemplate))
        }

        @JvmOverloads
        fun add(name: String?, linkFactory: Function<LibraryVersion?, String?>?, packages: Array<String?>? = null) {
            add(null, name, linkFactory, packages)
        }

        fun add(
            rootName: String?, name: String?, linkFactory: Function<LibraryVersion?, String?>?,
            packages: Array<String?>?
        ) {
            val link = Library.Link(rootName, linkFactory, if (packages != null) List.of<String?>(*packages) else null)
            this.links.computeIfAbsent(name) { key: kotlin.String? -> java.util.ArrayList<org.springframework.boot.build.bom.Library.Link?>() }!!
                .add(link)
        }

        fun asFactory(linkTemplate: String): Function<LibraryVersion?, String?> {
            return Function { version: LibraryVersion? ->
                val resolver =
                    PropertyPlaceholderHelper.PlaceholderResolver { name: String? -> if ("version" == name) version.toString() else null }
                PropertyPlaceholderHelper("{", "}").replacePlaceholders(linkTemplate, resolver)
            }
        }
    }

    class UpgradeHandler @Inject constructor(project: Project) {
        var upgradePolicy: UpgradePolicy? = null

        val gitHub: GitHubHandler

        init {
            this.gitHub = GitHubHandler(project)
        }

        fun setPolicy(upgradePolicy: UpgradePolicy?) {
            this.upgradePolicy = upgradePolicy
        }

        fun gitHub(action: Action<GitHubHandler?>) {
            action.execute(this.gitHub)
        }
    }

    class Upgrade constructor(val policy: UpgradePolicy?, val gitHub: GitHub?)

    class GitHubHandler(project: Project) {
        var organization: String?

        var repository: String?

        var issueLabels: MutableList<String?>? = null

        init {
            val buildProperties = BuildProperties.get(project)
            this.organization = buildProperties.gitHub.organization
            this.repository = buildProperties.gitHub.repository
        }

        fun setOrganization(organization: String?) {
            this.organization = organization
        }

        fun setRepository(repository: String?) {
            this.repository = repository
        }

        fun setIssueLabels(issueLabels: MutableList<String?>?) {
            this.issueLabels = issueLabels
        }
    }

    class GitHub constructor(
        val organization: String?,
        val repository: String?,
        val issueLabels: MutableList<String?>?
    )
}
