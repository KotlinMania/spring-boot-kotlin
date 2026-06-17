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
package org.springframework.boot.build.bom.bomr.version

import org.junit.jupiter.api.Test

/**
 * Tests for [ArtifactVersionDependencyVersion].
 * 
 * @author Andy Wilkinson
 */
internal class ArtifactVersionDependencyVersionTests {
    @Test
    fun parseWhenVersionIsNotAMavenVersionShouldReturnNull() {
        assertThat(version("1.2.3.1")).isNull()
    }

    @Test
    fun parseWhenVersionIsAMavenVersionShouldReturnAVersion() {
        assertThat(version("1.2.3")).isNotNull()
    }

    @get:Test
    val isSameMajorWhenSameMajorAndMinorShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.2").isSameMajor(version("1.10.0"))).isTrue()
        }

    @get:Test
    val isSameMajorWhenSameMajorShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.2").isSameMajor(version("1.9.0"))).isTrue()
        }

    @get:Test
    val isSameMajorWhenDifferentMajorShouldReturnFalse: Unit
        get() {
            assertThat(version("2.0.2").isSameMajor(version("1.9.0"))).isFalse()
        }

    @get:Test
    val isSameMinorWhenSameMinorShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.2").isSameMinor(version("1.10.1"))).isTrue()
        }

    @get:Test
    val isSameMinorWhenDifferentMinorShouldReturnFalse: Unit
        get() {
            assertThat(version("1.10.2").isSameMinor(version("1.9.1"))).isFalse()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForReleaseShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.2-SNAPSHOT").isSnapshotFor(version("1.10.2"))).isTrue()
        }

    @get:Test
    val isSnapshotForWhenBuildSnapshotForReleaseShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.2.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.RELEASE"))).isTrue()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForReleaseCandidateShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.2-SNAPSHOT").isSnapshotFor(version("1.10.2-RC2"))).isTrue()
        }

    @get:Test
    val isSnapshotForWhenBuildSnapshotForReleaseCandidateShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.2.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.RC2"))).isTrue()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForMilestoneShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.2-SNAPSHOT").isSnapshotFor(version("1.10.2-M1"))).isTrue()
        }

    @get:Test
    val isSnapshotForWhenBuildSnapshotForMilestoneShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.2.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.M1"))).isTrue()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForDifferentReleaseShouldReturnFalse: Unit
        get() {
            assertThat(version("1.10.1-SNAPSHOT").isSnapshotFor(version("1.10.2"))).isFalse()
        }

    @get:Test
    val isSnapshotForWhenBuildSnapshotForDifferentReleaseShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.1.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.RELEASE"))).isFalse()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForDifferentReleaseCandidateShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.1-SNAPSHOT").isSnapshotFor(version("1.10.2-RC2"))).isFalse()
        }

    @get:Test
    val isSnapshotForWhenBuildSnapshotForDifferentReleaseCandidateShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.1.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.RC2"))).isFalse()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForDifferentMilestoneShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.1-SNAPSHOT").isSnapshotFor(version("1.10.2-M1"))).isFalse()
        }

    @get:Test
    val isSnapshotForWhenBuildSnapshotForDifferentMilestoneShouldReturnTrue: Unit
        get() {
            assertThat(version("1.10.1.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.M1"))).isFalse()
        }

    @get:Test
    val isSnapshotForWhenNotSnapshotShouldReturnFalse: Unit
        get() {
            assertThat(version("1.10.1-M1").isSnapshotFor(version("1.10.1"))).isFalse()
        }

    private fun version(version: String?): ArtifactVersionDependencyVersion {
        return ArtifactVersionDependencyVersion.parse(version)
    }
}
