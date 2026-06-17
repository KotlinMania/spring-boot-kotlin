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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.build.bom.Library.BomAlignment
import org.springframework.boot.build.bom.Library.Group
import org.springframework.boot.build.bom.Library.LibraryVersion
import org.springframework.boot.build.bom.Library.Link
import org.springframework.boot.build.bom.Library.ProhibitedVersion
import org.springframework.boot.build.bom.Library.VersionAlignment
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import java.util.Collections

/**
 * Tests for [Library].
 * 
 * @author Phillip Webb
 */
internal class LibraryTests {
    @get:Test
    val linkRootNameWhenNoneSpecified: Unit
        get() {
            val name = "Spring Framework"
            val calendarName: String? = null
            val version: LibraryVersion = LibraryVersion(DependencyVersion.parse("1.2.3"))
            val groups: List<Group?>? = Collections.emptyList()
            val prohibitedVersion: List<ProhibitedVersion?>? = Collections.emptyList()
            val considerSnapshots = false
            val versionAlignment: VersionAlignment? = null
            val alignsWithBom: BomAlignment? = null
            val linkRootName: String? = null
            val links: Map<String?, List<Link?>?>? = Collections.emptyMap()
            val library: Library = Library(
                name, calendarName, version, groups, null, prohibitedVersion, considerSnapshots,
                versionAlignment, alignsWithBom, linkRootName, links
            )
            assertThat(library.getLinkRootName()).isEqualTo("spring-framework")
        }

    @get:Test
    val linkRootNameWhenSpecified: Unit
        get() {
            val name = "Spring Data BOM"
            val calendarName: String? = null
            val version: LibraryVersion = LibraryVersion(DependencyVersion.parse("1.2.3"))
            val groups: List<Group?>? = Collections.emptyList()
            val prohibitedVersion: List<ProhibitedVersion?>? = Collections.emptyList()
            val considerSnapshots = false
            val versionAlignment: VersionAlignment? = null
            val alignsWithBom: BomAlignment? = null
            val linkRootName = "spring-data"
            val links: Map<String?, List<Link?>?>? = Collections.emptyMap()
            val library: Library = Library(
                name, calendarName, version, groups, null, prohibitedVersion, considerSnapshots,
                versionAlignment, alignsWithBom, linkRootName, links
            )
            assertThat(library.getLinkRootName()).isEqualTo("spring-data")
        }

    @Test
    fun toMajorMinorGenerationWithRelease() {
        val version: LibraryVersion = LibraryVersion(DependencyVersion.parse("1.2.3"))
        assertThat(version.forMajorMinorGeneration()).isEqualTo("1.2.x")
    }

    @Test
    fun toMajorMinorGenerationWithSnapshot() {
        val version: LibraryVersion = LibraryVersion(DependencyVersion.parse("2.0.0-SNAPSHOT"))
        assertThat(version.forMajorMinorGeneration()).isEqualTo("2.0.x-SNAPSHOT")
    }
}
