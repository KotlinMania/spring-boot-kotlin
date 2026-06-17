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
 * Tests for [CalendarVersionDependencyVersion].
 * 
 * @author Andy Wilkinson
 */
internal class CalendarVersionDependencyVersionTests {
    @Test
    fun parseWhenVersionIsNotACalendarVersionShouldReturnNull() {
        assertThat(version("1.2.3")).isNull()
    }

    @Test
    fun parseWhenVersionIsACalendarVersionShouldReturnAVersion() {
        assertThat(version("2020.0.0")).isNotNull()
    }

    @get:Test
    val isSameMajorWhenSameMajorAndMinorShouldReturnTrue: Unit
        get() {
            assertThat(version("2020.0.0").isSameMajor(version("2020.0.1"))).isTrue()
        }

    @get:Test
    val isSameMajorWhenSameMajorShouldReturnTrue: Unit
        get() {
            assertThat(version("2020.0.0").isSameMajor(version("2020.1.0"))).isTrue()
        }

    @get:Test
    val isSameMajorWhenDifferentMajorShouldReturnFalse: Unit
        get() {
            assertThat(version("2020.0.0").isSameMajor(version("2021.0.0"))).isFalse()
        }

    @get:Test
    val isSameMinorWhenSameMinorShouldReturnTrue: Unit
        get() {
            assertThat(version("2020.0.0").isSameMinor(version("2020.0.1"))).isTrue()
        }

    @get:Test
    val isSameMinorWhenDifferentMinorShouldReturnFalse: Unit
        get() {
            assertThat(version("2020.0.0").isSameMinor(version("2020.1.0"))).isFalse()
        }

    @Test
    fun calendarVersionIsNotSameMajorAsReleaseTrainVersion() {
        assertThat(version("2020.0.0").isSameMajor(releaseTrainVersion("Aluminium-RELEASE"))).isFalse()
    }

    @Test
    fun calendarVersionIsNotSameMinorAsReleaseTrainVersion() {
        assertThat(version("2020.0.0").isSameMinor(releaseTrainVersion("Aluminium-RELEASE"))).isFalse()
    }

    private fun releaseTrainVersion(version: String?): ReleaseTrainDependencyVersion {
        return ReleaseTrainDependencyVersion.parse(version)
    }

    private fun version(version: String?): CalendarVersionDependencyVersion {
        return CalendarVersionDependencyVersion.parse(version)
    }
}
