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
package org.springframework.boot.build.artifacts

import org.assertj.core.api.Assertions
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.springframework.boot.build.artifacts.ArtifactRelease.Companion.forProject

/**
 * Tests for [ArtifactRelease].
 * 
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
internal class ArtifactReleaseTests {
    @Test
    fun whenProjectVersionIsSnapshotThenTypeIsSnapshot() {
        val project = ProjectBuilder.builder().build()
        project.setVersion("1.2.3-SNAPSHOT")
        Assertions.assertThat(forProject(project).getType()).isEqualTo("snapshot")
    }

    @Test
    fun whenProjectVersionIsMilestoneThenTypeIsMilestone() {
        val project = ProjectBuilder.builder().build()
        project.setVersion("1.2.3-M1")
        Assertions.assertThat(forProject(project).getType()).isEqualTo("milestone")
    }

    @Test
    fun whenProjectVersionIsReleaseCandidateThenTypeIsMilestone() {
        val project = ProjectBuilder.builder().build()
        project.setVersion("1.2.3-RC1")
        Assertions.assertThat(forProject(project).getType()).isEqualTo("milestone")
    }

    @Test
    fun whenProjectVersionIsReleaseThenTypeIsRelease() {
        val project = ProjectBuilder.builder().build()
        project.setVersion("1.2.3")
        Assertions.assertThat(forProject(project).getType()).isEqualTo("release")
    }

    @Test
    fun whenProjectVersionIsSnapshotThenRepositoryIsArtifactorySnapshot() {
        val project = ProjectBuilder.builder().build()
        project.setVersion("1.2.3-SNAPSHOT")
        Assertions.assertThat(forProject(project).downloadRepo).contains("repo.spring.io/snapshot")
    }

    @Test
    fun whenProjectVersionIsMilestoneThenRepositoryIsMavenCentral() {
        val project = ProjectBuilder.builder().build()
        project.setVersion("4.0.0-M1")
        Assertions.assertThat(forProject(project).downloadRepo)
            .contains("https://repo.maven.apache.org/maven2")
    }

    @Test
    fun whenProjectVersionIsReleaseCandidateThenRepositoryIsMavenCentral() {
        val project = ProjectBuilder.builder().build()
        project.setVersion("4.0.0-RC1")
        Assertions.assertThat(forProject(project).downloadRepo)
            .contains("https://repo.maven.apache.org/maven2")
    }

    @Test
    fun whenProjectVersionIsReleaseThenRepositoryIsMavenCentral() {
        val project = ProjectBuilder.builder().build()
        project.setVersion("1.2.3")
        Assertions.assertThat(forProject(project).downloadRepo)
            .contains("https://repo.maven.apache.org/maven2")
    }
}
