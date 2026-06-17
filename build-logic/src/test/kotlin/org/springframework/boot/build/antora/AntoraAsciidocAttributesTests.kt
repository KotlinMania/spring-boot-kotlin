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
package org.springframework.boot.build.antora

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.build.bom.Library
import org.springframework.boot.build.bom.Library.*
import org.springframework.boot.build.bom.bomr.version.DependencyVersion.Companion.parse
import org.springframework.boot.build.properties.BuildType
import java.util.List
import java.util.Map
import java.util.function.Function

/**
 * Tests for [AntoraAsciidocAttributes].
 * 
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
internal class AntoraAsciidocAttributesTests {
    @Test
    fun buildTypeWhenOpenSource() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3", true, BuildType.OPEN_SOURCE, null,
            mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("build-type", "opensource")
    }

    @Test
    fun buildTypeWhenCommercial() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3", true, BuildType.COMMERCIAL, null,
            mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("build-type", "commercial")
    }

    @Test
    fun githubRefWhenReleasedVersionIsTag() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3", true, BuildType.OPEN_SOURCE, null,
            mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("github-ref", "v1.2.3")
    }

    @Test
    fun githubRefWhenLatestSnapshotVersionIsMainBranch() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3-SNAPSHOT", true,
            BuildType.OPEN_SOURCE, null, mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("github-ref", "main")
    }

    @Test
    fun githubRefWhenOlderSnapshotVersionIsBranch() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3-SNAPSHOT", false,
            BuildType.OPEN_SOURCE, null, mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("github-ref", "1.2.x")
    }

    @Test
    fun githubRefWhenOlderSnapshotHotFixVersionIsBranch() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3.1-SNAPSHOT", false,
            BuildType.OPEN_SOURCE, null, mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("github-ref", "1.2.3.x")
    }

    @Test
    fun versionReferenceFromLibrary() {
        val library = mockLibrary(mutableMapOf<String?, MutableList<Library.Link?>?>())
        val attributes = AntoraAsciidocAttributes(
            "1.2.3.1-SNAPSHOT", false,
            BuildType.OPEN_SOURCE, List.of<Library?>(library), mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("version-spring-framework", "1.2.3")
    }

    @Test
    fun versionReferenceFromSpringDataDependencyReleaseVersion() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3", true, BuildType.OPEN_SOURCE, null,
            mockDependencyVersions("3.2.5"), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("version-spring-data-mongodb", "3.2.5")
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry(
            "url-spring-data-mongodb-docs",
            "https://docs.spring.io/spring-data/mongodb/reference/3.2"
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry(
            "url-spring-data-mongodb-javadoc",
            "https://docs.spring.io/spring-data/mongodb/docs/3.2.x/api"
        )
    }

    @Test
    fun versionReferenceFromSpringDataDependencySnapshotVersion() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3", true, BuildType.OPEN_SOURCE, null,
            mockDependencyVersions("3.2.0-SNAPSHOT"), null
        )
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("version-spring-data-mongodb", "3.2.0-SNAPSHOT")
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry(
            "url-spring-data-mongodb-docs",
            "https://docs.spring.io/spring-data/mongodb/reference/3.2-SNAPSHOT"
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry(
            "url-spring-data-mongodb-javadoc",
            "https://docs.spring.io/spring-data/mongodb/docs/3.2.x/api"
        )
    }

    @Test
    fun versionNativeBuildTools() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3", true, BuildType.OPEN_SOURCE, null,
            mockDependencyVersions(), Map.of<String?, String?>("nativeBuildToolsVersion", "3.4.5")
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("version-native-build-tools", "3.4.5")
    }

    @Test
    fun urlArtifactRepositoryWhenRelease() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3", true, BuildType.OPEN_SOURCE, null,
            mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("url-artifact-repository", "https://repo.maven.apache.org/maven2")
    }

    @Test
    fun urlArtifactRepositoryWhenMilestone() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3-M1", true, BuildType.OPEN_SOURCE,
            null, mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("url-artifact-repository", "https://repo.maven.apache.org/maven2")
    }

    @Test
    fun urlArtifactRepositoryWhenSnapshot() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3-SNAPSHOT", true,
            BuildType.OPEN_SOURCE, null, mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("url-artifact-repository", "https://repo.spring.io/snapshot")
    }

    @Test
    fun artifactReleaseTypeWhenOpenSourceRelease() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3", true, BuildType.OPEN_SOURCE, null,
            mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("artifact-release-type", "release")
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("build-and-artifact-release-type", "opensource-release")
    }

    @Test
    fun artifactReleaseTypeWhenOpenSourceMilestone() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3-M1", true, BuildType.OPEN_SOURCE,
            null, mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("artifact-release-type", "milestone")
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("build-and-artifact-release-type", "opensource-milestone")
    }

    @Test
    fun artifactReleaseTypeWhenOpenSourceSnapshot() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3-SNAPSHOT", true,
            BuildType.OPEN_SOURCE, null, mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("artifact-release-type", "snapshot")
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("build-and-artifact-release-type", "opensource-snapshot")
    }

    @Test
    fun artifactReleaseTypeWhenCommercialRelease() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3", true, BuildType.COMMERCIAL, null,
            mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("artifact-release-type", "release")
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("build-and-artifact-release-type", "commercial-release")
    }

    @Test
    fun artifactReleaseTypeWhenCommercialMilestone() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3-M1", true, BuildType.COMMERCIAL, null,
            mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("artifact-release-type", "milestone")
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("build-and-artifact-release-type", "commercial-milestone")
    }

    @Test
    fun artifactReleaseTypeWhenCommercialSnapshot() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3-SNAPSHOT", true, BuildType.COMMERCIAL,
            null, mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get()).containsEntry("artifact-release-type", "snapshot")
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("build-and-artifact-release-type", "commercial-snapshot")
    }

    @Test
    fun urlLinksFromLibrary() {
        val links: MutableMap<String?, MutableList<Library.Link?>?> =
            LinkedHashMap<String?, MutableList<Library.Link?>?>()
        links.put("site", singleLink(Function { version: LibraryVersion? -> "https://example.com/site/" + version }))
        links.put("docs", singleLink(Function { version: LibraryVersion? -> "https://example.com/docs/" + version }))
        links.put(
            "javadoc",
            singleLink(
                Function { version: LibraryVersion? -> "https://example.com/api/" + version },
                "org.springframework.[core|util]"
            )
        )
        val library = mockLibrary(links)
        val attributes = AntoraAsciidocAttributes(
            "1.2.3.1-SNAPSHOT", false,
            BuildType.OPEN_SOURCE, List.of<Library?>(library), mockDependencyVersions(), null
        )
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("url-spring-framework-site", "https://example.com/site/1.2.3")
            .containsEntry("url-spring-framework-docs", "https://example.com/docs/1.2.3")
            .containsEntry("url-spring-framework-javadoc", "https://example.com/api/1.2.3")
        Assertions.assertThat<String?, String?>(attributes.get())
            .containsEntry("javadoc-location-org-springframework-core", "{url-spring-framework-javadoc}")
            .containsEntry("javadoc-location-org-springframework-util", "{url-spring-framework-javadoc}")
    }

    private fun singleLink(
        factory: Function<LibraryVersion?, String?>?,
        vararg packages: String?
    ): MutableList<Library.Link?> {
        val link = Library.Link(null, factory, List.of<String?>(*packages))
        return List.of<Library.Link?>(link)
    }

    @Test
    fun linksFromProperties() {
        val attributes = AntoraAsciidocAttributes(
            "1.2.3-SNAPSHOT", true, BuildType.OPEN_SOURCE,
            null, mockDependencyVersions(), null
        )
            .get()
        Assertions.assertThat<String?, String?>(attributes)
            .containsEntry("include-java", "ROOT:example\$java/org/springframework/boot/docs")
        Assertions.assertThat<String?, String?>(attributes).containsEntry(
            "url-spring-data-cassandra-site",
            "https://spring.io/projects/spring-data-cassandra"
        )
        val keys: MutableList<String?> = ArrayList<String?>(attributes.keys)
        Assertions.assertThat(keys.indexOf("include-java")).isLessThan(keys.indexOf("code-spring-boot-latest"))
    }

    private fun mockLibrary(links: MutableMap<String?, MutableList<Library.Link?>?>?): Library {
        val name = "Spring Framework"
        val calendarName: String? = null
        val version = LibraryVersion(parse("1.2.3"))
        val groups = mutableListOf<Library.Group?>()
        val prohibitedVersion = mutableListOf<ProhibitedVersion?>()
        val considerSnapshots = false
        val versionAlignment: VersionAlignment? = null
        val alignsWithBom: BomAlignment? = null
        val linkRootName: String? = null
        val library = Library(
            name, calendarName, version, groups, null, prohibitedVersion, considerSnapshots,
            versionAlignment, alignsWithBom, linkRootName, links
        )
        return library
    }

    private fun mockDependencyVersions(version: String? = "1.2.3"): MutableMap<String?, String?> {
        val versions: MutableMap<String?, String?> = LinkedHashMap<String?, String?>()
        addMockSpringDataVersion(versions, "spring-data-commons", version)
        addMockSpringDataVersion(versions, "spring-data-cassandra", version)
        addMockSpringDataVersion(versions, "spring-data-couchbase", version)
        addMockSpringDataVersion(versions, "spring-data-elasticsearch", version)
        addMockSpringDataVersion(versions, "spring-data-jdbc", version)
        addMockSpringDataVersion(versions, "spring-data-jpa", version)
        addMockSpringDataVersion(versions, "spring-data-mongodb", version)
        addMockSpringDataVersion(versions, "spring-data-neo4j", version)
        addMockSpringDataVersion(versions, "spring-data-r2dbc", version)
        addMockSpringDataVersion(versions, "spring-data-redis", version)
        addMockSpringDataVersion(versions, "spring-data-rest-core", version)
        addMockSpringDataVersion(versions, "spring-data-ldap", version)
        addMockTestcontainersVersion(versions, "activemq", version)
        addMockTestcontainersVersion(versions, "cassandra", version)
        addMockTestcontainersVersion(versions, "clickhouse", version)
        addMockTestcontainersVersion(versions, "couchbase", version)
        addMockTestcontainersVersion(versions, "elasticsearch", version)
        addMockTestcontainersVersion(versions, "grafana", version)
        addMockTestcontainersVersion(versions, "jdbc", version)
        addMockTestcontainersVersion(versions, "kafka", version)
        addMockTestcontainersVersion(versions, "mariadb", version)
        addMockTestcontainersVersion(versions, "mongodb", version)
        addMockTestcontainersVersion(versions, "mssqlserver", version)
        addMockTestcontainersVersion(versions, "mysql", version)
        addMockTestcontainersVersion(versions, "neo4j", version)
        addMockTestcontainersVersion(versions, "oracle-xe", version)
        addMockTestcontainersVersion(versions, "oracle-free", version)
        addMockTestcontainersVersion(versions, "postgresql", version)
        addMockTestcontainersVersion(versions, "pulsar", version)
        addMockTestcontainersVersion(versions, "rabbitmq", version)
        addMockTestcontainersVersion(versions, "redpanda", version)
        addMockTestcontainersVersion(versions, "r2dbc", version)
        addMockJackson2CoreVersion(versions, "jackson-annotations", version)
        addMockJackson2CoreVersion(versions, "jackson-databind", version)
        addMockJacksonCoreVersion(versions, "jackson-core", version)
        addMockJacksonCoreVersion(versions, "jackson-databind", version)
        addMockJacksonCoreVersion(versions, "jackson-databind", version)
        versions.put("io.grpc:grpc-api", version)
        versions.put("org.apache.pulsar:pulsar-client-api", version)
        versions.put("tools.jackson.dataformat:jackson-dataformat-xml", version)
        return versions
    }

    private fun addMockSpringDataVersion(
        versions: MutableMap<String?, String?>,
        artifactId: String?,
        version: String?
    ) {
        versions.put("org.springframework.data:" + artifactId, version)
    }

    private fun addMockTestcontainersVersion(
        versions: MutableMap<String?, String?>,
        artifactId: String?,
        version: String?
    ) {
        versions.put("org.testcontainers:" + artifactId, version)
    }

    private fun addMockJackson2CoreVersion(
        versions: MutableMap<String?, String?>,
        artifactId: String?,
        version: String?
    ) {
        versions.put("com.fasterxml.jackson.core:" + artifactId, version)
    }

    private fun addMockJacksonCoreVersion(
        versions: MutableMap<String?, String?>,
        artifactId: String?,
        version: String?
    ) {
        versions.put("tools.jackson.core:" + artifactId, version)
    }
}
