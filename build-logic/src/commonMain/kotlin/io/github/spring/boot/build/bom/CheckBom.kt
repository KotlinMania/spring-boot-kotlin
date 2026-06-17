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

import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.springframework.boot.build.bom.Library.PermittedDependency
import org.springframework.boot.build.bom.Library.VersionAlignment
import org.springframework.boot.build.bom.ResolvedBom.Bom
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import java.io.File
import java.lang.String
import java.util.*
import java.util.List
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Collectors
import javax.inject.Inject
import kotlin.Boolean
import kotlin.RuntimeException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.plus
import kotlin.map
import kotlin.plus
import kotlin.sequences.map
import kotlin.sequences.plus
import kotlin.text.StringBuilder
import kotlin.text.indexOf
import kotlin.text.map
import kotlin.text.plus
import kotlin.text.startsWith
import kotlin.toString
import org.gradle.api.file.RegularFileProperty

/**
 * Checks the validity of a bom.
 * 
 * @author Andy Wilkinson
 * @author Wick Dynex
 */
abstract class CheckBom @Inject constructor(bom: BomExtension) : DefaultTask() {
    private val bom: BomExtension

    private val checks: MutableList<LibraryCheck?>

    init {
        val configurations = project.getConfigurations()
        val dependencies = project.getDependencies()
        val resolvedBom: Provider<ResolvedBom> =
            this.resolvedBomFile.map<File>(Transformer { obj: RegularFile? -> obj!!.asFile }).map<ResolvedBom>(
                Transformer { file: File? -> ResolvedBom.Companion.readFrom(file) })
        this.checks = List.of<LibraryCheck?>(
            CheckExclusions(configurations, dependencies), CheckProhibitedVersions(),
            CheckVersionAlignment(),
            CheckDependencyManagementAlignment(resolvedBom, configurations, dependencies),
            CheckForUnwantedDependencyManagement(resolvedBom)
        )
        this.bom = bom
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val resolvedBomFile: RegularFileProperty

    @TaskAction
    fun checkBom() {
        val errors: MutableList<String?> = ArrayList<String?>()
        for (library in this.bom.libraries) {
            errors.addAll(checkLibrary(library))
        }
        if (!errors.isEmpty()) {
            println()
            errors.forEach(Consumer { x: String? -> println(x) })
            println()
            throw VerificationException("Bom check failed. See previous output for details.")
        }
    }

    private fun checkLibrary(library: Library): MutableList<String?> {
        val libraryErrors: MutableList<String?> = ArrayList<String?>()
        this.checks.stream().flatMap<String> { check: LibraryCheck? -> check!!.check(library)!!.stream() }
            .forEach { e: String? -> libraryErrors.add(e) }
        val errors: MutableList<String?> = ArrayList<String?>()
        if (!libraryErrors.isEmpty()) {
            errors.add(library.name)
            for (libraryError in libraryErrors) {
                errors.add("    - " + libraryError)
            }
        }
        return errors
    }

    private interface LibraryCheck {
        fun check(library: Library?): MutableList<String?>?
    }

    private class CheckExclusions(
        private val configurations: ConfigurationContainer,
        private val dependencies: DependencyHandler
    ) : LibraryCheck {
        override fun check(library: Library): MutableList<String?> {
            val errors: MutableList<String?> = ArrayList<String?>()
            for (group in library.groups) {
                for (module in group.modules) {
                    if (!module.exclusions.isEmpty()) {
                        checkExclusions(group.id, module, library.version.version, errors)
                    }
                }
            }
            return errors
        }

        fun checkExclusions(
            groupId: String?,
            module: Library.Module,
            version: DependencyVersion?,
            errors: MutableList<String?>
        ) {
            val resolved = this.configurations
                .detachedConfiguration(this.dependencies.create(groupId + ":" + module.name + ":" + version))
                .getResolvedConfiguration()
                .getResolvedArtifacts()
                .stream()
                .map<ModuleVersionIdentifier> { artifact: ResolvedArtifact? -> artifact!!.getModuleVersion().id }
                .map<String> { id: ModuleVersionIdentifier? -> id!!.getGroup() + ":" + id.getModule().name }
                .collect(Collectors.toSet())
            val exclusions = module.exclusions
                .stream()
                .map<String> { exclusion: Library.Exclusion? -> exclusion!!.groupId + ":" + exclusion.artifactId }
                .collect(Collectors.toSet())
            val unused: MutableSet<String?> = TreeSet<String?>()
            for (exclusion in exclusions) {
                if (!resolved.contains(exclusion)) {
                    if (exclusion.endsWith(":*")) {
                        val group = exclusion.substring(0, exclusion.indexOf(':') + 1)
                        if (resolved.stream().noneMatch { candidate: String? -> candidate!!.startsWith(group) }) {
                            unused.add(exclusion)
                        }
                    } else {
                        unused.add(exclusion)
                    }
                }
            }
            exclusions.removeAll(resolved)
            if (!unused.isEmpty()) {
                errors.add("Unnecessary exclusions on " + groupId + ":" + module.name + ": " + exclusions)
            }
        }
    }

    private class CheckProhibitedVersions : LibraryCheck {
        override fun check(library: Library): MutableList<String?> {
            val errors: MutableList<String?> = ArrayList<String?>()
            val currentVersion: ArtifactVersion = DefaultArtifactVersion(library.version.version.toString())
            for (prohibited in library.prohibitedVersions) {
                if (prohibited.isProhibited(library.version.version.toString())) {
                    errors.add("Current version " + currentVersion + " is prohibited")
                } else {
                    val versionRange = prohibited.range
                    if (versionRange != null) {
                        check(currentVersion, versionRange, errors)
                    }
                }
            }
            return errors
        }

        fun check(currentVersion: ArtifactVersion, versionRange: VersionRange, errors: MutableList<String?>) {
            for (restriction in versionRange.getRestrictions()) {
                val upperBound = restriction.getUpperBound()
                if (upperBound == null) {
                    return
                }
                val comparison = currentVersion.compareTo(upperBound)
                if ((restriction.isUpperBoundInclusive() && comparison <= 0)
                    || ((!restriction.isUpperBoundInclusive()) && comparison < 0)
                ) {
                    return
                }
            }
            errors.add(
                ("Version range " + versionRange + " is ineffective as the current version, " + currentVersion
                        + ", is greater than its upper bound")
            )
        }
    }

    private class CheckVersionAlignment : LibraryCheck {
        override fun check(library: Library): MutableList<String?> {
            val errors: MutableList<String?> = ArrayList<String?>()
            val versionAlignment = library.versionAlignment
            if (versionAlignment != null) {
                check(versionAlignment, library, errors)
            }
            return errors
        }

        fun check(versionAlignment: VersionAlignment, library: Library, errors: MutableList<String?>) {
            val alignedVersions = versionAlignment.resolve()
            if (alignedVersions.size == 1) {
                val alignedVersion = alignedVersions.iterator().next()
                if (alignedVersion != library.version.version.toString()) {
                    errors.add(
                        ("Version " + library.version.version + " is misaligned. It should be "
                                + alignedVersion + ".")
                    )
                }
            } else {
                if (alignedVersions.isEmpty()) {
                    errors.add("Version alignment requires a single version but none were found.")
                } else {
                    errors.add(
                        ("Version alignment requires a single version but " + alignedVersions.size
                                + " were found: " + alignedVersions + ".")
                    )
                }
            }
        }
    }

    private abstract class ResolvedLibraryCheck(private val resolvedBom: Provider<ResolvedBom>) : LibraryCheck {
        override fun check(library: Library): MutableList<String?>? {
            val resolvedLibrary = getResolvedLibrary(library)
            return check(library, resolvedLibrary)
        }

        protected abstract fun check(
            library: Library?,
            resolvedLibrary: ResolvedBom.ResolvedLibrary?
        ): MutableList<String?>?

        fun getResolvedLibrary(library: Library): ResolvedBom.ResolvedLibrary {
            val resolvedBom = this.resolvedBom.get()
            val resolvedLibrary = resolvedBom.libraries
                .stream()
                .filter { candidate: ResolvedBom.ResolvedLibrary? -> candidate!!.name == library.name }
                .findFirst()
            if (!resolvedLibrary.isPresent()) {
                throw RuntimeException("Library '%s' not found in resolved bom".format(library.name))
            }
            return resolvedLibrary.get()
        }
    }

    private class CheckDependencyManagementAlignment(
        resolvedBom: Provider<ResolvedBom>,
        configurations: ConfigurationContainer?, dependencies: DependencyHandler?
    ) : ResolvedLibraryCheck(resolvedBom) {
        private val bomResolver: BomResolver

        init {
            this.bomResolver = BomResolver(configurations, dependencies)
        }

        public override fun check(
            library: Library,
            resolvedLibrary: ResolvedBom.ResolvedLibrary
        ): MutableList<String?> {
            val errors: MutableList<String?> = ArrayList<String?>()
            val alignsWithBom = library.alignsWithBom
            if (alignsWithBom != null) {
                val mavenBom = this.bomResolver
                    .resolveMavenBom(alignsWithBom.coordinates + ":" + library.version.version)
                checkDependencyManagementAlignment(
                    resolvedLibrary,
                    mavenBom,
                    errors,
                    Predicate { id: ResolvedBom.Id? -> alignsWithBom.exclude(id) })
            }
            return errors
        }

        fun checkDependencyManagementAlignment(
            library: ResolvedBom.ResolvedLibrary, mavenBom: Bom, errors: MutableList<String?>,
            excluded: Predicate<ResolvedBom.Id?>
        ) {
            val managedByLibrary = library.managedDependencies
            val managedByBom = managedDependenciesOf(mavenBom)

            val missing: MutableList<ResolvedBom.Id?> = ArrayList<ResolvedBom.Id?>(managedByBom)
            missing.removeIf(excluded)
            missing.removeAll(managedByLibrary)

            val unexpected: MutableList<ResolvedBom.Id?> = ArrayList<ResolvedBom.Id?>(managedByLibrary)
            unexpected.removeAll(managedByBom)
            if (missing.isEmpty() && unexpected.isEmpty()) {
                return
            }
            var error = "Dependency management does not align with " + mavenBom.id + ":"
            if (!missing.isEmpty()) {
                error = error + "%n        - Missing:%n            %s".format(
                    String.join(
                        "\n            ",
                        missing.stream().map<kotlin.String> { dependency: ResolvedBom.Id? -> dependency.toString() }
                            .toList()))
            }
            if (!unexpected.isEmpty()) {
                error = error + "%n        - Unexpected:%n            %s".format(
                    String.join(
                        "\n            ",
                        unexpected.stream().map<kotlin.String> { dependency: ResolvedBom.Id? -> dependency.toString() }
                            .toList()))
            }
            errors.add(error)
        }

        fun managedDependenciesOf(mavenBom: Bom): MutableList<ResolvedBom.Id?> {
            val managedDependencies: MutableList<ResolvedBom.Id?> = ArrayList<ResolvedBom.Id?>()
            managedDependencies.addAll(mavenBom.managedDependencies)
            if (mavenBom.parent != null) {
                managedDependencies.addAll(managedDependenciesOf(mavenBom.parent))
            }
            for (importedBom in mavenBom.importedBoms) {
                managedDependencies.addAll(managedDependenciesOf(importedBom))
            }
            return managedDependencies
        }
    }

    private class CheckForUnwantedDependencyManagement(resolvedBom: Provider<ResolvedBom>) :
        ResolvedLibraryCheck(resolvedBom) {
        public override fun check(
            library: Library,
            resolvedLibrary: ResolvedBom.ResolvedLibrary
        ): MutableList<kotlin.String?> {
            val unwanted = findUnwantedDependencyManagement(library, resolvedLibrary)
            val errors: MutableList<kotlin.String?> = ArrayList<kotlin.String?>()
            if (!unwanted.isEmpty()) {
                val error = StringBuilder("Unwanted dependency management:")
                unwanted.forEach { (bom: kotlin.String?, dependencies: MutableSet<kotlin.String?>?) ->
                    error.append("%n        - %s:".format(bom))
                    error.append("%n            - %s".format(String.join("\n            - ", dependencies)))
                }
                errors.add(error.toString())
            }
            val unnecessary = findUnnecessaryPermittedDependencies(library, resolvedLibrary)
            if (!unnecessary.isEmpty()) {
                val error = StringBuilder("Dependencies permitted unnecessarily:")
                unnecessary.forEach { (bom: kotlin.String?, dependencies: MutableSet<kotlin.String?>?) ->
                    error.append("%n        - %s:".format(bom))
                    error.append("%n            - %s".format(String.join("\n            - ", dependencies)))
                }
                errors.add(error.toString())
            }
            return errors
        }

        fun findUnwantedDependencyManagement(
            library: Library,
            resolvedLibrary: ResolvedBom.ResolvedLibrary
        ): MutableMap<kotlin.String?, MutableSet<kotlin.String?>?> {
            val unwanted: MutableMap<kotlin.String?, MutableSet<kotlin.String?>?> =
                LinkedHashMap<kotlin.String?, MutableSet<kotlin.String?>?>()
            for (bom in resolvedLibrary.importedBoms) {
                val notPermitted: MutableSet<kotlin.String?> = TreeSet<kotlin.String?>()
                val managedDependencies = managedDependenciesOf(bom)
                managedDependencies.stream()
                    .filter { dependency: ResolvedBom.Id? ->
                        unwanted(
                            bom,
                            dependency!!,
                            findPermittedDependencies(library, bom)!!
                        )
                    }
                    .map<kotlin.String> { obj: ResolvedBom.Id? -> obj.toString() }
                    .forEach { e: kotlin.String? -> notPermitted.add(e) }
                if (!notPermitted.isEmpty()) {
                    unwanted.put(bom.id.artifactId, notPermitted)
                }
            }
            return unwanted
        }

        fun findPermittedDependencies(library: Library, bom: Bom): MutableList<PermittedDependency>? {
            for (group in library.groups) {
                for (importedBom in group.boms) {
                    if (importedBom.name == bom.id.artifactId && group.id == bom.id.groupId) {
                        return importedBom.permittedDependencies
                    }
                }
            }
            return mutableListOf<PermittedDependency?>()
        }

        fun managedDependenciesOf(bom: Bom?): MutableSet<ResolvedBom.Id?> {
            val managedDependencies: MutableSet<ResolvedBom.Id?> = TreeSet<ResolvedBom.Id?>()
            if (bom != null) {
                managedDependencies.addAll(bom.managedDependencies)
                managedDependencies.addAll(managedDependenciesOf(bom.parent))
                for (importedBom in bom.importedBoms) {
                    managedDependencies.addAll(managedDependenciesOf(importedBom))
                }
            }
            return managedDependencies
        }

        fun unwanted(
            bom: Bom,
            managedDependency: ResolvedBom.Id,
            permittedDependencies: MutableList<PermittedDependency>
        ): Boolean {
            if (bom.id.groupId == managedDependency.groupId
                || managedDependency.groupId.startsWith(bom.id.groupId + ".")
            ) {
                return false
            }
            for (permittedDependency in permittedDependencies) {
                if (permittedDependency.artifactId == managedDependency.artifactId
                    && permittedDependency.groupId == managedDependency.groupId
                ) {
                    return false
                }
            }
            return true
        }

        fun findUnnecessaryPermittedDependencies(
            library: Library,
            resolvedLibrary: ResolvedBom.ResolvedLibrary
        ): MutableMap<kotlin.String?, MutableSet<kotlin.String?>?> {
            val unnecessary: MutableMap<kotlin.String?, MutableSet<kotlin.String?>?> =
                HashMap<kotlin.String?, MutableSet<kotlin.String?>?>()
            for (bom in resolvedLibrary.importedBoms) {
                val permittedDependencies: MutableSet<kotlin.String?> =
                    findPermittedDependencies(library, bom)!!.stream()
                        .map<kotlin.String> { dependency: PermittedDependency? -> dependency!!.groupId + ":" + dependency.artifactId }
                        .collect(Collectors.toCollection(Supplier { TreeSet() }))
                val dependencies: MutableSet<kotlin.String?> = managedDependenciesOf(bom).stream()
                    .map<kotlin.String> { dependency: ResolvedBom.Id? -> dependency!!.groupId + ":" + dependency.artifactId }
                    .collect(Collectors.toCollection(Supplier { TreeSet() }))
                permittedDependencies.removeAll(dependencies)
                if (!permittedDependencies.isEmpty()) {
                    unnecessary.put(bom.id.artifactId, permittedDependencies)
                }
            }
            return unnecessary
        }
    }
}
