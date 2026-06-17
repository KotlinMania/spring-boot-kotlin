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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.build.bom.Library
import org.springframework.boot.build.bom.bomr.VersionOption.AlignedVersionOption
import org.springframework.boot.build.bom.bomr.VersionOption.ResolvedVersionOption
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import java.time.Duration
import java.util.*
import java.util.function.BiFunction

/**
 * Standard implementation for [LibraryUpdateResolver].
 * 
 * @author Andy Wilkinson
 */
internal class StandardLibraryUpdateResolver(
    private val versionResolver: VersionResolver,
    private val versionOptionResolver: BiFunction<Library?, DependencyVersion?, VersionOption?>
) : LibraryUpdateResolver {
    override fun findLibraryUpdates(
        librariesToUpgrade: MutableCollection<Library>,
        librariesByName: MutableMap<String?, Library?>?
    ): MutableList<LibraryWithVersionOptions?> {
        val result: MutableList<LibraryWithVersionOptions?> = ArrayList<LibraryWithVersionOptions?>()
        for (library in librariesToUpgrade) {
            if (isLibraryExcluded(library)) {
                continue
            }
            logger.info("Looking for updates for {}", library.name)
            val start = System.nanoTime()
            val versionOptions = getVersionOptions(library)
            result.add(LibraryWithVersionOptions(library, versionOptions))
            logger.info(
                "Found {} updates for {}, took {}", versionOptions.size, library.name,
                Duration.ofNanos(System.nanoTime() - start)
            )
        }
        return result
    }

    protected fun isLibraryExcluded(library: Library): Boolean {
        return library.name == "Spring Boot"
    }

    protected fun getVersionOptions(library: Library): MutableList<VersionOption?> {
        val options: MutableList<VersionOption?> = ArrayList<VersionOption?>()
        val alignedOption = determineAlignedVersionOption(library)
        if (alignedOption != null) {
            options.add(alignedOption)
        }
        for (resolvedOption in determineResolvedVersionOptions(library)) {
            if (alignedOption == null || alignedOption.getVersion() != resolvedOption.getVersion()) {
                options.add(resolvedOption)
            }
        }
        return options
    }

    private fun determineAlignedVersionOption(library: Library): VersionOption? {
        val versionAlignment = library.versionAlignment
        if (versionAlignment != null) {
            val alignedVersions = versionAlignment.resolve()
            if (alignedVersions != null && alignedVersions.size == 1) {
                val alignedVersion = DependencyVersion.parse(alignedVersions.iterator().next())
                if (alignedVersion != library.version!!.version) {
                    return AlignedVersionOption(alignedVersion, versionAlignment)
                }
            }
        }
        return null
    }

    private fun determineResolvedVersionOptions(library: Library): MutableList<VersionOption> {
        val moduleVersions: MutableMap<String?, SortedSet<DependencyVersion?>?> =
            LinkedHashMap<String?, SortedSet<DependencyVersion?>?>()
        for (group in library.groups!!) {
            for (module in group.modules!!) {
                moduleVersions.put(
                    group.id + ":" + module.name,
                    getLaterVersionsForModule(group.id, module.name, library)
                )
            }
            for (bom in group.boms!!) {
                moduleVersions.put(
                    group.id + ":" + bom,
                    getLaterVersionsForModule(group.id, bom!!.name, library)
                )
            }
            for (plugin in group.plugins!!) {
                moduleVersions.put(
                    group.id + ":" + plugin,
                    getLaterVersionsForModule(group.id, plugin, library)
                )
            }
        }
        val versionOptions: MutableList<VersionOption> = ArrayList<VersionOption>()
        moduleVersions.values.stream()
            .flatMap<DependencyVersion?> { obj: SortedSet<DependencyVersion?>? -> obj!!.stream() }.distinct()
            .forEach { dependencyVersion: DependencyVersion? ->
                var versionOption = this.versionOptionResolver.apply(library, dependencyVersion)
                if (versionOption != null) {
                    val missingModules = getMissingModules(moduleVersions, dependencyVersion)
                    if (!missingModules.isEmpty()) {
                        versionOption = ResolvedVersionOption(versionOption.getVersion(), missingModules)
                    }
                    versionOptions.add(versionOption)
                }
            }
        return versionOptions
    }

    private fun getMissingModules(
        moduleVersions: MutableMap<String?, SortedSet<DependencyVersion?>?>,
        version: DependencyVersion?
    ): MutableList<String?> {
        val missingModules: MutableList<String?> = ArrayList<String?>()
        moduleVersions.forEach { (name: String?, versions: SortedSet<DependencyVersion?>?) ->
            if (!versions!!.contains(version)) {
                missingModules.add(name)
            }
        }
        return missingModules
    }

    private fun getLaterVersionsForModule(
        groupId: String?,
        artifactId: String?,
        library: Library?
    ): SortedSet<DependencyVersion?>? {
        return this.versionResolver.resolveVersions(groupId, artifactId)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(StandardLibraryUpdateResolver::class.java)
    }
}
