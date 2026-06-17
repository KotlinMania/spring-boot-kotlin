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

import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.internal.tasks.userinput.UserQuestions
import org.springframework.boot.build.bom.Library
import org.springframework.boot.build.bom.bomr.VersionOption.AlignedVersionOption
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import java.util.*
import java.util.function.Function

/**
 * Interactive [UpgradeResolver] that uses command line input to choose the upgrades
 * to apply.
 * 
 * @author Andy Wilkinson
 */
class InteractiveUpgradeResolver internal constructor(
    private val userInputHandler: UserInputHandler,
    private val libraryUpdateResolver: LibraryUpdateResolver
) : UpgradeResolver {
    override fun resolveUpgrades(
        librariesToUpgrade: MutableCollection<Library?>?,
        libraries: MutableCollection<Library>
    ): MutableList<Upgrade?> {
        val librariesByName: MutableMap<String?, Library?> = HashMap<String?, Library?>()
        for (library in libraries) {
            librariesByName.put(library.name, library)
        }
        try {
            return this.libraryUpdateResolver.findLibraryUpdates(librariesToUpgrade, librariesByName)
                .stream()
                .map<Upgrade?> { libraryWithVersionOptions: LibraryWithVersionOptions? ->
                    this.resolveUpgrade(
                        libraryWithVersionOptions!!
                    )
                }
                .filter { obj: Upgrade? -> Objects.nonNull(obj) }
                .toList()
        } catch (ex: UpgradesInterruptedException) {
            return mutableListOf<Upgrade?>()
        }
    }

    private fun resolveUpgrade(libraryWithVersionOptions: LibraryWithVersionOptions): Upgrade? {
        val library = libraryWithVersionOptions.getLibrary()
        val versionOptions = libraryWithVersionOptions.getVersionOptions()
        if (versionOptions.isEmpty()) {
            return null
        }
        val defaultOption = defaultOption(library)
        val selected = selectOption(defaultOption, library, versionOptions)
        return if (selected == defaultOption) null else selected.upgrade(library)
    }

    private fun defaultOption(library: Library): VersionOption {
        val alignment = library.versionAlignment
        val alignedVersions = if (alignment != null) alignment.resolve() else null
        if (alignedVersions != null && alignedVersions.size == 1) {
            val alignedVersion = DependencyVersion.parse(alignedVersions.iterator().next())
            if (alignedVersion == library.version!!.version) {
                return AlignedVersionOption(alignedVersion, alignment)
            }
        }
        return VersionOption(library.version!!.version)
    }

    private fun selectOption(
        defaultOption: VersionOption, library: Library,
        versionOptions: MutableList<VersionOption?>
    ): VersionOption {
        val selected = this.userInputHandler.askUser<VersionOption>(Function { questions: UserQuestions? ->
            val question = library.nameAndVersion
            val options: MutableList<VersionOption?> = ArrayList<VersionOption?>()
            options.add(defaultOption)
            options.addAll(versionOptions)
            questions!!.selectOption<VersionOption?>(question, options, defaultOption)
        }).get()
        if (this.userInputHandler.interrupted()) {
            throw UpgradesInterruptedException()
        }
        return selected
    }

    internal class UpgradesInterruptedException : RuntimeException()
}
