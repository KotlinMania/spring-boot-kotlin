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
 * Tests for [DependencyVersion].
 * 
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
internal class DependencyVersionTests {
    @Test
    fun parseWhenValidMavenVersionShouldReturnArtifactVersionDependencyVersion() {
        assertThat(DependencyVersion.parse("1.2.3.Final")).isExactlyInstanceOf(ArtifactVersionDependencyVersion::class.java)
    }

    @Test
    fun parseWhenReleaseTrainShouldReturnReleaseTrainDependencyVersion() {
        assertThat(DependencyVersion.parse("Ingalls-SR5")).isInstanceOf(ReleaseTrainDependencyVersion::class.java)
    }

    @Test
    fun parseWhenMavenLikeVersionWithNumericQualifierShouldReturnNumericQualifierDependencyVersion() {
        assertThat(DependencyVersion.parse("1.2.3.4")).isInstanceOf(MultipleComponentsDependencyVersion::class.java)
    }

    @Test
    fun parseWhen5ComponentsShouldReturnNumericQualifierDependencyVersion() {
        assertThat(DependencyVersion.parse("1.2.3.4.5")).isInstanceOf(MultipleComponentsDependencyVersion::class.java)
    }

    @Test
    fun parseWhenVersionWithLeadingZeroesShouldReturnLeadingZeroesDependencyVersion() {
        assertThat(DependencyVersion.parse("1.4.01")).isInstanceOf(LeadingZeroesDependencyVersion::class.java)
    }

    @Test
    fun parseWhenVersionWithCombinedPatchAndQualifierShouldReturnCombinedPatchAndQualifierDependencyVersion() {
        assertThat(DependencyVersion.parse("4.0.0M4")).isInstanceOf(CombinedPatchAndQualifierDependencyVersion::class.java)
    }

    @Test
    fun parseWhenCalendarVersionShouldReturnArtifactVersionDependencyVersion() {
        assertThat(DependencyVersion.parse("2020.0.0")).isInstanceOf(CalendarVersionDependencyVersion::class.java)
    }

    @Test
    fun parseWhenCalendarVersionWithModifierShouldReturnArtifactVersionDependencyVersion() {
        assertThat(DependencyVersion.parse("2020.0.0-M1")).isInstanceOf(CalendarVersionDependencyVersion::class.java)
    }
}
