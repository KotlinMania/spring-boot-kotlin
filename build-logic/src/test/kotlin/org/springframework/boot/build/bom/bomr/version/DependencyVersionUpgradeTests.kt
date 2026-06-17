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

/**
 * Tests for [DependencyVersion.isUpgrade] of [DependencyVersion]
 * implementations.
 * 
 * @author Andy Wilkinson
 */
internal class DependencyVersionUpgradeTests {
    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3", candidate = "1.2.3")
    @ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.3.RELEASE")
    @CalendarVersion(current = "2023.0.0", candidate = "2023.0.0")
    @ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-RELEASE")
    fun isUpgradeWhenSameVersionShouldReturnFalse(current: DependencyVersion, candidate: DependencyVersion?) {
        assertThat(current.isUpgrade(candidate, false)).isFalse()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0-SNAPSHOT")
    @ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenSameSnapshotVersionShouldReturnFalse(current: DependencyVersion, candidate: DependencyVersion?) {
        assertThat(current.isUpgrade(candidate, false)).isFalse()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0-SNAPSHOT")
    @ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenSameSnapshotVersionAndMovingToSnapshotsShouldReturnFalse(
        current: DependencyVersion,
        candidate: DependencyVersion?
    ) {
        assertThat(current.isUpgrade(candidate, true)).isFalse()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3", candidate = "1.2.4")
    @ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.4.RELEASE")
    @CalendarVersion(current = "2023.0.0", candidate = "2023.0.1")
    @ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-SR1")
    fun isUpgradeWhenLaterPatchReleaseShouldReturnTrue(current: DependencyVersion, candidate: DependencyVersion?) {
        assertThat(current.isUpgrade(candidate, false)).isTrue()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3", candidate = "1.2.4-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.4.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0", candidate = "2023.0.1-SNAPSHOT")
    fun isUpgradeWhenSnapshotOfLaterPatchReleaseShouldReturnTrue(
        current: DependencyVersion,
        candidate: DependencyVersion?
    ) {
        assertThat(current.isUpgrade(candidate, false)).isTrue()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3", candidate = "1.2.4-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.4.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0", candidate = "2023.0.1-SNAPSHOT")
    @ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenSnapshotOfLaterPatchReleaseAndMovingToSnapshotsShouldReturnTrue(
        current: DependencyVersion,
        candidate: DependencyVersion?
    ) {
        assertThat(current.isUpgrade(candidate, true)).isTrue()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3", candidate = "1.2.3-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.3.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0", candidate = "2023.0.0-SNAPSHOT")
    @ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenSnapshotOfSameVersionShouldReturnFalse(current: DependencyVersion, candidate: DependencyVersion?) {
        assertThat(current.isUpgrade(candidate, false)).isFalse()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3-M2")
    @ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.M2")
    @CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0-M2")
    @ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-M2")
    fun isUpgradeWhenSnapshotToMilestoneShouldReturnTrue(current: DependencyVersion, candidate: DependencyVersion?) {
        assertThat(current.isUpgrade(candidate, false)).isTrue()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3-RC1")
    @ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.RC1")
    @CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0-RC1")
    @ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-RC1")
    fun isUpgradeWhenSnapshotToReleaseCandidateShouldReturnTrue(
        current: DependencyVersion,
        candidate: DependencyVersion?
    ) {
        assertThat(current.isUpgrade(candidate, false)).isTrue()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3-SNAPSHOT", candidate = "1.2.3")
    @ArtifactVersion(current = "1.2.3.BUILD-SNAPSHOT", candidate = "1.2.3.RELEASE")
    @CalendarVersion(current = "2023.0.0-SNAPSHOT", candidate = "2023.0.0")
    @ReleaseTrain(current = "Kay-BUILD-SNAPSHOT", candidate = "Kay-RELEASE")
    fun isUpgradeWhenSnapshotToReleaseShouldReturnTrue(current: DependencyVersion, candidate: DependencyVersion?) {
        assertThat(current.isUpgrade(candidate, false)).isTrue()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3-M1", candidate = "1.2.3-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.M1", candidate = "1.2.3.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0-M1", candidate = "2023.0.0-SNAPSHOT")
    @ReleaseTrain(current = "Kay-M1", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenMilestoneToSnapshotShouldReturnFalse(current: DependencyVersion, candidate: DependencyVersion?) {
        assertThat(current.isUpgrade(candidate, false)).isFalse()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3-RC1", candidate = "1.2.3-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.RC1", candidate = "1.2.3.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0-RC1", candidate = "2023.0.0-SNAPSHOT")
    @ReleaseTrain(current = "Kay-RC1", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenReleaseCandidateToSnapshotShouldReturnFalse(
        current: DependencyVersion,
        candidate: DependencyVersion?
    ) {
        assertThat(current.isUpgrade(candidate, false)).isFalse()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3", candidate = "1.2.3-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.3.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0", candidate = "2023.0.0-SNAPSHOT")
    @ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenReleaseToSnapshotShouldReturnFalse(current: DependencyVersion, candidate: DependencyVersion?) {
        assertThat(current.isUpgrade(candidate, false)).isFalse()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3-M1", candidate = "1.2.3-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.M1", candidate = "1.2.3.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0-M1", candidate = "2023.0.0-SNAPSHOT")
    @ReleaseTrain(current = "Kay-M1", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenMilestoneToSnapshotAndMovingToSnapshotsShouldReturnTrue(
        current: DependencyVersion,
        candidate: DependencyVersion?
    ) {
        assertThat(current.isUpgrade(candidate, true)).isTrue()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3-RC1", candidate = "1.2.3-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.RC1", candidate = "1.2.3.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0-RC1", candidate = "2023.0.0-SNAPSHOT")
    @ReleaseTrain(current = "Kay-RC1", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenReleaseCandidateToSnapshotAndMovingToSnapshotsShouldReturnTrue(
        current: DependencyVersion,
        candidate: DependencyVersion?
    ) {
        assertThat(current.isUpgrade(candidate, true)).isTrue()
    }

    @ParameterizedTest
    @ArtifactVersion(current = "1.2.3", candidate = "1.2.3-SNAPSHOT")
    @ArtifactVersion(current = "1.2.3.RELEASE", candidate = "1.2.3.BUILD-SNAPSHOT")
    @CalendarVersion(current = "2023.0.0", candidate = "2023.0.0-SNAPSHOT")
    fun isUpgradeWhenReleaseToSnapshotAndMovingToSnapshotsShouldReturnFalse(
        current: DependencyVersion,
        candidate: DependencyVersion?
    ) {
        assertThat(current.isUpgrade(candidate, true)).isFalse()
    }

    @ParameterizedTest
    @ReleaseTrain(current = "Kay-RELEASE", candidate = "Kay-BUILD-SNAPSHOT")
    fun isUpgradeWhenReleaseTrainToSnapshotAndMovingToSnapshotsShouldReturnTrue(
        current: DependencyVersion,
        candidate: DependencyVersion?
    ) {
        assertThat(current.isUpgrade(candidate, true)).isTrue()
    }

    @Repeatable(org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.ArtifactVersions::class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ArgumentsSource(org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.InputProvider::class)
    internal annotation class ArtifactVersion(val current: String, val candidate: String)

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    internal annotation class ArtifactVersions(vararg val value: ArtifactVersion)

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ArgumentsSource(org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.InputProvider::class)
    internal annotation class ReleaseTrain(val current: String, val candidate: String)

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ArgumentsSource(org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.InputProvider::class)
    internal annotation class CalendarVersion(val current: String, val candidate: String)

    internal class InputProvider : ArgumentsProvider {
        @Override
        fun provideArguments(
            parameterDeclarations: ParameterDeclarations?,
            context: ExtensionContext
        ): Stream<out Arguments?> {
            val testMethod: Method = context.getRequiredTestMethod()
            val artifactVersions: Stream<Arguments?>? = artifactVersions(testMethod)
                .map({ artifactVersion ->
                    Arguments.of(
                        org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.VersionType.ARTIFACT_VERSION.parse(
                            artifactVersion.current()
                        ),
                        org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.VersionType.ARTIFACT_VERSION.parse(
                            artifactVersion.candidate()
                        )
                    )
                })
            val releaseTrains: Stream<Arguments?>? = releaseTrains(testMethod)
                .map({ releaseTrain ->
                    Arguments.of(
                        org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.VersionType.RELEASE_TRAIN.parse(
                            releaseTrain.current()
                        ),
                        org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.VersionType.RELEASE_TRAIN.parse(
                            releaseTrain.candidate()
                        )
                    )
                })
            val calendarVersions: Stream<Arguments?>? = calendarVersions(testMethod)
                .map({ calendarVersion ->
                    Arguments.of(
                        org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.VersionType.CALENDAR_VERSION.parse(
                            calendarVersion.current()
                        ),
                        org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.VersionType.CALENDAR_VERSION.parse(
                            calendarVersion.candidate()
                        )
                    )
                })
            return Stream.concat(Stream.concat(artifactVersions, releaseTrains), calendarVersions)
        }

        private fun artifactVersions(testMethod: Method): Stream<ArtifactVersion?> {
            val artifactVersions: ArtifactVersions? =
                testMethod.getAnnotation(org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.ArtifactVersions::class.java)
            if (artifactVersions != null) {
                return Stream.of(artifactVersions.value)
            }
            return versions<T?>(testMethod, ArtifactVersion::class.java)
        }

        private fun releaseTrains(testMethod: Method): Stream<ReleaseTrain?> {
            return versions<T?>(
                testMethod,
                org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.ReleaseTrain::class.java
            )
        }

        private fun calendarVersions(testMethod: Method): Stream<CalendarVersion?> {
            return versions<T?>(
                testMethod,
                org.springframework.boot.build.bom.bomr.version.DependencyVersionUpgradeTests.CalendarVersion::class.java
            )
        }

        private fun <T : Annotation?> versions(testMethod: Method, type: Class<T?>?): Stream<T?> {
            val annotation: T? = testMethod.getAnnotation(type)
            return if (annotation != null) Stream.of(annotation) else Stream.empty()
        }
    }

    internal enum class VersionType(parser: Function<String?, DependencyVersion?>) {
        ARTIFACT_VERSION(ArtifactVersionDependencyVersion::parse),

        CALENDAR_VERSION(CalendarVersionDependencyVersion::parse),

        RELEASE_TRAIN(ReleaseTrainDependencyVersion::parse);

        private val parser: Function<String?, DependencyVersion?>

        init {
            this.parser = parser
        }

        fun parse(version: String?): DependencyVersion {
            return this.parser.apply(version)
        }
    }
}
