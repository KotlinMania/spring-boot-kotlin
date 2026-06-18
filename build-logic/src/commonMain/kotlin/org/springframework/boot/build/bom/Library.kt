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

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import org.springframework.boot.build.xml.XmlDocument
import java.io.File
import java.util.*
import java.util.List
import java.util.Set
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Stream
import javax.xml.xpath.XPathFactory

/**
 * A collection of modules, Maven plugins, and Maven boms that are versioned and released
 * together.
 * 
 * @author Andy Wilkinson
 */
class Library(
    val name: String,
    calendarName: String?,
    val version: LibraryVersion?,
    val groups: MutableList<Group>?,
    val upgradePolicy: UpgradePolicy?,
    val prohibitedVersions: MutableList<ProhibitedVersion?>?,
    val isConsiderSnapshots: Boolean,
    val versionAlignment: VersionAlignment?,
    val alignsWithBom: BomAlignment?,
    linkRootName: String?,
    links: MutableMap<String?, MutableList<Link?>?>?
) {
    val calendarName: String?

    val versionProperty: String?

    val linkRootName: String

    val links: MutableMap<String?, MutableList<Link?>?>

    /**
     * Create a new `Library` with the given `name`, `version`, and
     * `groups`.
     * @param name name of the library
     * @param calendarName name of the library as it appears in the Spring Calendar. May
     * be `null` in which case the `name` is used.
     * @param version version of the library
     * @param groups groups in the library
     * @param upgradePolicy the upgrade policy of the library, or `null` to use the
     * containing bom's policy
     * @param prohibitedVersions version of the library that are prohibited
     * @param considerSnapshots whether to consider snapshots
     * @param versionAlignment version alignment, if any, for the library
     * @param bomAlignment the bom, if any, that this library should align with
     * @param linkRootName the root name to use when generating link variable or
     * `null` to generate one based on the library `name`
     * @param links a list of HTTP links relevant to the library
     */
    init {
        this.calendarName = if (calendarName != null) calendarName else name
        this.versionProperty = if ("Spring Boot" == name)
            null
        else
            name.lowercase().replace(' ', '-') + ".version"
        this.linkRootName = if (linkRootName != null) linkRootName else generateLinkRootName(
            name
        )
        this.links = if (links != null) Collections.unmodifiableMap<String?, MutableList<Link?>?>(
            TreeMap<String?, MutableList<Link?>?>(links)
        ) else mutableMapOf<String?, MutableList<Link?>?>()
    }

    fun getLinkUrl(name: String?): String? {
        val links = getLinks(name)
        if (links == null || links.isEmpty()) {
            return null
        }
        check(links.size <= 1) { "Expected a single '%s' link for %s".formatted(name, this.name) }
        return links.get(0)!!.url(this)
    }

    fun getLinks(name: String?): MutableList<Link?>? {
        return this.links.get(name)
    }

    val nameAndVersion: String
        get() = this.name + " " + this.version

    fun withVersion(version: LibraryVersion?): Library {
        return Library(
            this.name, this.calendarName, version, this.groups, this.upgradePolicy,
            this.prohibitedVersions, this.isConsiderSnapshots, this.versionAlignment, this.alignsWithBom,
            this.linkRootName, this.links
        )
    }

    /**
     * A version or range of versions that are prohibited from being used in a bom.
     */
    class ProhibitedVersion(
        val range: VersionRange?, val startsWith: MutableList<String?>, val endsWith: MutableList<String?>,
        val contains: MutableList<String?>, val reason: String?
    ) {
        fun isProhibited(candidate: String): Boolean {
            var result = false
            result = result
                    || (this.range != null && this.range.containsVersion(DefaultArtifactVersion(candidate)))
            result = result || this.startsWith.stream().anyMatch { prefix: String? -> candidate.startsWith(prefix!!) }
            result = result || this.endsWith.stream().anyMatch { suffix: String? -> candidate.endsWith(suffix!!) }
            result = result || this.contains.stream().anyMatch { s: String? -> candidate.contains(s!!) }
            return result
        }
    }

    class LibraryVersion(val version: DependencyVersion) {
        fun componentInts(): IntArray? {
            return Arrays.stream<String?>(parts()).mapToInt { s: String? -> s!!.toInt() }.toArray()
        }

        fun major(): String? {
            return parts()[0]
        }

        fun minor(): String? {
            return parts()[1]
        }

        fun patch(): String? {
            return parts()[2]
        }

        override fun toString(): String {
            return this.version.toString()
        }

        fun toString(separator: String): String {
            return this.version.toString().replace(".", separator)
        }

        fun forAntora(): String {
            val parts = parts()
            var result = parts[0] + "." + parts[1]
            if (toString().endsWith("SNAPSHOT")) {
                result += "-SNAPSHOT"
            }
            return result
        }

        fun forMajorMinorGeneration(): String {
            val parts = parts()
            var result = parts[0] + "." + parts[1] + ".x"
            if (toString().endsWith("SNAPSHOT")) {
                result += "-SNAPSHOT"
            }
            return result
        }

        private fun parts(): Array<String?> {
            return toString().split("[.-]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }
    }

    /**
     * A collection of modules, Maven plugins, and Maven boms with the same group ID.
     */
    class Group(
        val id: String?,
        val modules: MutableList<Module>?,
        val plugins: MutableList<String?>?,
        val boms: MutableList<ImportedBom?>?
    )

    /**
     * A module in a group.
     */
    class Module @JvmOverloads constructor(
        val name: String?,
        val type: String?,
        classifier: String? = null,
        val exclusions: MutableList<Exclusion?>? = mutableListOf<Exclusion?>()
    ) {
        val classifier: String

        @JvmOverloads
        constructor(name: String?, exclusions: MutableList<Exclusion?>? = mutableListOf<Exclusion?>()) : this(
            name,
            null,
            null,
            exclusions
        )

        init {
            this.classifier = if (classifier != null) classifier else ""
        }
    }

    /**
     * An exclusion of a dependency identified by its group ID and artifact ID.
     */
    class Exclusion(val groupId: String?, val artifactId: String?)

    interface VersionAlignment {
        fun resolve(): MutableSet<String?>?

        fun alignmentConfiguration(project: Project, dependencies: MutableCollection<Dependency?>): Configuration {
            val alignmentConfiguration = project.getConfigurations()
                .detachedConfiguration(*dependencies.toTypedArray<Dependency?>())
            alignmentConfiguration.getResolutionStrategy().cacheChangingModulesFor(0, TimeUnit.SECONDS)
            return alignmentConfiguration
        }
    }

    class BomAlignment(val coordinates: String?, private val excluding: Predicate<ResolvedBom.Id?>) {
        fun exclude(id: ResolvedBom.Id?): Boolean {
            return this.excluding.test(id)
        }
    }

    /**
     * Version alignment for a library based on a dependency of another module.
     */
    class DependencyVersionAlignment internal constructor(
        private val dependency: String?, val from: String, val managedBy: String?, private val project: Project,
        private val libraries: MutableList<Library>, private val groups: MutableList<Group>
    ) : VersionAlignment {
        private var alignedVersions: MutableSet<String?>? = null

        override fun resolve(): MutableSet<String?> {
            if (this.alignedVersions != null) {
                return this.alignedVersions!!
            }
            val versions = resolveAligningDependencies()
            if (this.dependency != null) {
                val version = versions.get(this.dependency)
                this.alignedVersions = if (version != null) Set.of<String?>(version) else mutableSetOf<String?>()
            } else {
                val versionsInLibrary: MutableSet<String?> = HashSet<String?>()
                for (group in this.groups) {
                    for (module in group.modules!!) {
                        val version = versions.get(group.id + ":" + module.name)
                        if (version != null) {
                            versionsInLibrary.add(version)
                        }
                    }
                    for (plugin in group.plugins!!) {
                        val version = versions.get(group.id + ":" + plugin)
                        if (version != null) {
                            versionsInLibrary.add(version)
                        }
                    }
                }
                this.alignedVersions = versionsInLibrary
            }
            return this.alignedVersions!!
        }

        private fun resolveAligningDependencies(): MutableMap<String?, String?> {
            val dependencies =
                this.aligningDependencies
            val alignmentConfiguration = alignmentConfiguration(this.project, dependencies)
            val versions: MutableMap<String?, String?> = HashMap<String?, String?>()
            val resolutionResult = alignmentConfiguration.getIncoming().getResolutionResult()
            for (dependency in resolutionResult.getAllDependencies()) {
                versions.put(
                    dependency.getFrom().getModuleVersion()!!.getModule().toString(),
                    dependency.getFrom().getModuleVersion()!!.getVersion()
                )
            }
            return versions
        }

        private val aligningDependencies: MutableList<Dependency?>
            get() {
                if (this.managedBy == null) {
                    val fromLibrary = findFromLibrary()
                    return List
                        .of<Dependency?>(
                            this.project.getDependencies().create(this.from + ":" + fromLibrary!!.version!!.version)
                        )
                } else {
                    val managingLibrary = findManagingLibrary()
                    val boms =
                        getBomDependencies(managingLibrary)
                    val dependencies: MutableList<Dependency?> =
                        ArrayList<Dependency?>()
                    dependencies.addAll(boms)
                    dependencies.add(this.project.getDependencies().create(this.from))
                    return dependencies
                }
            }

        private fun findFromLibrary(): Library? {
            for (library in this.libraries) {
                for (group in library.groups!!) {
                    for (module in group.modules!!) {
                        if (this.from == group.id + ":" + module.name) {
                            return library
                        }
                    }
                }
            }
            return null
        }

        private fun findManagingLibrary(): Library? {
            if (this.managedBy == null) {
                return null
            }
            return this.libraries.stream()
                .filter { candidate: Library? -> this.managedBy == candidate!!.name }
                .findFirst()
                .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("Managing library '" + this.managedBy + "' not found.") })
        }

        private fun getBomDependencies(manager: Library?): MutableList<Dependency?> {
            if (manager == null) {
                return mutableListOf<Dependency?>()
            }
            return manager.groups!!
                .stream()
                .flatMap<Dependency?> { group: Group? ->
                    group!!.boms!!
                        .stream()
                        .map<Dependency?> { bom: ImportedBom? ->
                            this.project.getDependencies()
                                .platform(group.id + ":" + bom!!.name + ":" + manager.version!!.version)
                        }
                }
                .toList()
        }

        override fun toString(): String {
            var result = "version from dependencies of " + this.from
            if (this.managedBy != null) {
                result += " that is managed by " + this.managedBy
            }
            return result
        }
    }

    /**
     * Version alignment for a library based on a property in the pom of another module.
     */
    class PomPropertyVersionAlignment internal constructor(
        private val name: String?,
        private val from: String?,
        private val managedBy: String?,
        private val project: Project,
        private val libraries: MutableList<Library?>
    ) : VersionAlignment {
        private val alignedVersions: MutableSet<String?>? = null

        override fun resolve(): MutableSet<String?> {
            if (this.alignedVersions != null) {
                return this.alignedVersions
            }
            val alignmentConfiguration = alignmentConfiguration(
                this.project,
                this.aligningDependencies
            )
            val files: MutableSet<File?> = alignmentConfiguration.resolve()
            check(files.size == 1) { "Expected a single file when resolving the pom of " + this.from + " but found " + files.size }
            val pomFile = files.iterator().next()
            return Set.of<String?>(propertyFrom(pomFile))
        }

        private val aligningDependencies: MutableList<Dependency?>
            get() {
                val managingLibrary = findManagingLibrary()
                val boms =
                    getBomDependencies(managingLibrary!!)
                val dependencies: MutableList<Dependency?> =
                    ArrayList<Dependency?>()
                dependencies.addAll(boms)
                dependencies.add(this.project.getDependencies().create(this.from + "@pom"))
                return dependencies
            }

        private fun findManagingLibrary(): Library? {
            if (this.managedBy == null) {
                return null
            }
            return this.libraries.stream()
                .filter { candidate: Library? -> this.managedBy == candidate!!.name }
                .findFirst()
                .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("Managing library '" + this.managedBy + "' not found.") })
        }

        private fun getBomDependencies(manager: Library): MutableList<Dependency?> {
            return manager.groups!!
                .stream()
                .flatMap<Dependency?> { group: Group? ->
                    group!!.boms!!
                        .stream()
                        .map<Dependency?> { bom: ImportedBom? ->
                            this.project.getDependencies()
                                .platform(group.id + ":" + bom!!.name + ":" + manager.version!!.version)
                        }
                }
                .toList()
        }

        private fun propertyFrom(pomFile: File?): String? {
            try {
                val document = XmlDocument.parse(pomFile)
                val xpath = XPathFactory.newInstance().newXPath()
                return xpath.evaluate("/project/properties/" + this.name + "/text()", document)
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }
        }

        override fun toString(): String {
            var result = "version from properties of " + this.from
            if (this.managedBy != null) {
                result += " that is managed by " + this.managedBy
            }
            return result
        }
    }

    class Link(
        val rootName: String?,
        val factory: Function<LibraryVersion?, String?>?,
        packages: MutableList<String?>?
    ) {
        fun url(library: Library): String? {
            return url(library.version)
        }

        fun url(libraryVersion: LibraryVersion?): String? {
            return this.factory!!.apply(libraryVersion)
        }

        val packages: MutableList<String?>?

        init {
            var packages = packages
            packages =
                if (packages != null) List.copyOf<String?>(expandPackages(packages)) else mutableListOf<String?>()
            this.packages = packages
        }

        companion object {
            private val PACKAGE_EXPAND: Pattern = Pattern.compile("^(.*)\\[(.*)\\]$")

            private fun expandPackages(packages: MutableList<String?>): MutableList<String?> {
                return packages.stream()
                    .flatMap<String?> { packageName: String? -> Companion.expandPackage(packageName!!) }.toList()
            }

            private fun expandPackage(packageName: String): Stream<String?> {
                val matcher: Matcher = PACKAGE_EXPAND.matcher(packageName)
                if (!matcher.matches()) {
                    return Stream.of<String?>(packageName)
                }
                val root = matcher.group(1)
                val suffixes: Array<String?> =
                    matcher.group(2).split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                return Stream.of<String?>(*suffixes).map<String?> { suffix: String? -> root + suffix }
            }
        }
    }

    @JvmRecord
    data class ImportedBom(val name: String?, val permittedDependencies: MutableList<PermittedDependency?>?) {
        constructor(name: String?) : this(name, mutableListOf<PermittedDependency?>())
    }

    @JvmRecord
    data class PermittedDependency(val groupId: String?, val artifactId: String?)

    companion object {
        private fun generateLinkRootName(name: String): String {
            return name.replace("-", "").replace(" ", "-").lowercase()
        }
    }
}
