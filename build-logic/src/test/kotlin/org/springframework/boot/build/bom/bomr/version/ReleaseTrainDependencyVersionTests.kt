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
 * Tests for [ReleaseTrainDependencyVersion].
 * 
 * @author Andy Wilkinson
 */
internal class ReleaseTrainDependencyVersionTests {
    @Test
    fun parsingOfANonReleaseTrainVersionReturnsNull() {
        assertThat(
            org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                "5.1.4.RELEASE"
            )
        ).isNull()
    }

    @Test
    fun parsingOfAReleaseTrainVersionReturnsVersion() {
        assertThat(
            org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                "Lovelace-SR3"
            )
        ).isNotNull()
    }

    @get:Test
    val isSameMajorWhenReleaseTrainIsDifferentShouldReturnFalse: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Lovelace-RELEASE"
                ).isSameMajor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Kay-SR5"
                    )
                )
            ).isFalse()
        }

    @get:Test
    val isSameMajorWhenReleaseTrainIsTheSameShouldReturnTrue: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Lovelace-RELEASE"
                ).isSameMajor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Lovelace-SR5"
                    )
                )
            ).isTrue()
        }

    @get:Test
    val isSameMinorWhenReleaseTrainIsDifferentShouldReturnFalse: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Lovelace-RELEASE"
                ).isSameMajor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Kay-SR5"
                    )
                )
            ).isFalse()
        }

    @get:Test
    val isSameMinorWhenReleaseTrainIsTheSameShouldReturnTrue: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Lovelace-RELEASE"
                ).isSameMajor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Lovelace-SR5"
                    )
                )
            ).isTrue()
        }

    @Test
    fun releaseTrainVersionIsNotSameMajorAsCalendarTrainVersion() {
        assertThat(
            org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                "Kay-SR6"
            ).isSameMajor(calendarVersion("2020.0.0"))
        ).isFalse()
    }

    @Test
    fun releaseTrainVersionIsNotSameMinorAsCalendarVersion() {
        assertThat(
            org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                "Kay-SR6"
            ).isSameMinor(calendarVersion("2020.0.0"))
        ).isFalse()
    }

    @get:Test
    val isSnapshotForWhenSnapshotForServiceReleaseShouldReturnTrue: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Kay-BUILD-SNAPSHOT"
                ).isSnapshotFor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Kay-SR2"
                    )
                )
            ).isTrue()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForReleaseShouldReturnTrue: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Kay-BUILD-SNAPSHOT"
                ).isSnapshotFor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Kay-RELEASE"
                    )
                )
            ).isTrue()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForReleaseCandidateShouldReturnTrue: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Kay-BUILD-SNAPSHOT"
                ).isSnapshotFor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Kay-RC1"
                    )
                )
            ).isTrue()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForMilestoneShouldReturnTrue: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Kay-BUILD-SNAPSHOT"
                ).isSnapshotFor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Kay-M2"
                    )
                )
            ).isTrue()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForDifferentReleaseShouldReturnFalse: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Kay-BUILD-SNAPSHOT"
                ).isSnapshotFor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Lovelace-RELEASE"
                    )
                )
            ).isFalse()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForDifferentReleaseCandidateShouldReturnTrue: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Kay-BUILD-SNAPSHOT"
                ).isSnapshotFor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Lovelace-RC2"
                    )
                )
            ).isFalse()
        }

    @get:Test
    val isSnapshotForWhenSnapshotForDifferentMilestoneShouldReturnTrue: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Kay-BUILD-SNAPSHOT"
                ).isSnapshotFor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Lovelace-M1"
                    )
                )
            ).isFalse()
        }

    @get:Test
    val isSnapshotForWhenNotSnapshotShouldReturnFalse: Unit
        get() {
            assertThat(
                org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                    "Kay-M1"
                ).isSnapshotFor(
                    org.springframework.boot.build.bom.bomr.version.ReleaseTrainDependencyVersionTests.Companion.version(
                        "Kay-RELEASE"
                    )
                )
            ).isFalse()
        }

    private fun calendarVersion(version: String?): CalendarVersionDependencyVersion {
        return CalendarVersionDependencyVersion.parse(version)
    }

    companion object {
        private fun version(input: String?): ReleaseTrainDependencyVersion {
            return ReleaseTrainDependencyVersion.parse(input)
        }
    }
}
