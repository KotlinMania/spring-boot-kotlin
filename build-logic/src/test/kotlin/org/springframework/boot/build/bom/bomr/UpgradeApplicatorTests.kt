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
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.build.bom.Library
import org.springframework.boot.build.bom.Library.LibraryVersion
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.util.Collections
import java.util.Properties

/**
 * Tests for [UpgradeApplicator].
 * 
 * @author Andy Wilkinson
 */
internal class UpgradeApplicatorTests {
    @TempDir
    var temp: File? = null

    @Test
    @kotlin.Throws(IOException::class)
    fun whenUpgradeIsAppliedToLibraryWithVersionThenBomIsUpdated() {
        val bom: File = File(this.temp, "bom.gradle.kts")
        FileCopyUtils.copy(File("src/test/resources/bom.gradle.kts"), bom)
        val originalContents: String = Files.readString(bom.toPath())
        val gradleProperties: File = File(this.temp, "gradle.properties")
        FileCopyUtils.copy(File("src/test/resources/gradle.properties"), gradleProperties)
        val activeMq: Library = Library(
            "ActiveMQ", null, LibraryVersion(DependencyVersion.parse("5.15.11")), null,
            null, null, false, null, null, null, Collections.emptyMap()
        )
        UpgradeApplicator(bom.toPath(), gradleProperties.toPath())
            .apply(Upgrade(activeMq, activeMq.withVersion(LibraryVersion(DependencyVersion.parse("5.16")))))
        val bomContents: String? = Files.readString(bom.toPath())
        assertThat(bomContents).hasSize(originalContents.length() - 3)
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun whenUpgradeIsAppliedToLibraryWithVersionPropertyThenGradlePropertiesIsUpdated() {
        val bom: File = File(this.temp, "bom.gradle.kts")
        FileCopyUtils.copy(File("src/test/resources/bom.gradle.kts"), bom)
        val gradleProperties: File = File(this.temp, "gradle.properties")
        FileCopyUtils.copy(File("src/test/resources/gradle.properties"), gradleProperties)
        val kotlin: Library = Library(
            "Kotlin", null, LibraryVersion(DependencyVersion.parse("1.3.70")), null, null,
            null, false, null, null, null, Collections.emptyMap()
        )
        UpgradeApplicator(bom.toPath(), gradleProperties.toPath())
            .apply(Upgrade(kotlin, kotlin.withVersion(LibraryVersion(DependencyVersion.parse("1.4")))))
        val properties: Properties = Properties()
        FileInputStream(gradleProperties).use { `in` ->
            properties.load(`in`)
        }
        assertThat(properties).containsOnly(
            entry("a", "alpha"), entry("b", "bravo"), entry("kotlinVersion", "1.4"),
            entry("t", "tango")
        )
    }
}
