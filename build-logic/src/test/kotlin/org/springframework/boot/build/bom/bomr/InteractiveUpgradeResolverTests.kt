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

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.provider.Provider
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.boot.build.bom.Library
import org.springframework.boot.build.bom.Library.LibraryVersion
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import java.util.ArrayList

/**
 * Tests for [InteractiveUpgradeResolver].
 * 
 * @author Phillip Webb
 */
internal class InteractiveUpgradeResolverTests {
    @Test
    fun resolveUpgradeUpdateVersionNumberInLibrary() {
        val userInputHandler: UserInputHandler = mock(UserInputHandler::class.java)
        val libaryUpdateResolver: LibraryUpdateResolver = mock(LibraryUpdateResolver::class.java)
        val upgradeResolver: InteractiveUpgradeResolver = InteractiveUpgradeResolver(
            userInputHandler,
            libaryUpdateResolver
        )
        val libraries: List<Library?> = ArrayList()
        val version: DependencyVersion? = DependencyVersion.parse("1.0.0")
        val libraryVersion: LibraryVersion = LibraryVersion(version)
        val library: Library = Library("test", null, libraryVersion, null, null, null, false, null, null, null, null)
        libraries.add(library)
        val librariesToUpgrade: List<Library?> = ArrayList()
        librariesToUpgrade.add(library)
        val updates: List<LibraryWithVersionOptions?> = ArrayList()
        val updateVersion: DependencyVersion? = DependencyVersion.parse("1.0.1")
        val versionOption: VersionOption = VersionOption(updateVersion)
        updates.add(LibraryWithVersionOptions(library, List.of(versionOption)))
        given(libaryUpdateResolver.findLibraryUpdates(any(), any())).willReturn(updates)
        val providerOfVersionOption: Provider<Object?> = providerOf<Any?>(versionOption)
        given(userInputHandler.askUser(any())).willReturn(providerOfVersionOption)
        val upgrades: List<Upgrade?> = upgradeResolver.resolveUpgrades(librariesToUpgrade, libraries)
        assertThat(upgrades.get(0).to().version.version).isEqualTo(updateVersion)
    }

    @SuppressWarnings(["unchecked", "rawtypes"])
    private fun <T> providerOf(versionOption: VersionOption?): Provider {
        val provider: Provider = mock(Provider::class.java)
        given(provider.get()).willReturn(versionOption)
        return provider
    }
}
