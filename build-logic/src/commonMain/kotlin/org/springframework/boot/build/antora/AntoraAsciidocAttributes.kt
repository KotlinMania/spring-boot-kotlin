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

import org.gradle.api.Project
import org.springframework.boot.build.artifacts.ArtifactRelease
import org.springframework.boot.build.bom.BomExtension
import org.springframework.boot.build.bom.Library
import org.springframework.boot.build.bom.ResolvedBom
import org.springframework.boot.build.bom.ResolvedBom.Bom
import org.springframework.boot.build.properties.BuildProperties
import org.springframework.boot.build.properties.BuildType
import org.springframework.util.Assert
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Collectors

/**
 * Generates Asciidoctor attributes for use with Antora.
 * 
 * @author Phillip Webb
 */
class AntoraAsciidocAttributes {
    private val version: String

    private val latestVersion: Boolean

    private val buildType: BuildType

    private val artifactRelease: ArtifactRelease

    private val libraries: MutableList<Library?>

    private val dependencyVersions: MutableMap<String?, String>

    private val projectProperties: MutableMap<String?, *>

    constructor(project: Project, dependencyBom: BomExtension, resolvedBom: ResolvedBom) {
        this.version = project.getVersion().toString()
        this.latestVersion = project.findProperty("latestVersion").toString().toBoolean()
        this.buildType = BuildProperties.get(project).buildType
        this.artifactRelease = ArtifactRelease.forProject(project)
        this.libraries = dependencyBom.getLibraries()
        this.dependencyVersions = dependencyVersionsOf(resolvedBom)
        this.projectProperties = project.getProperties()
    }

    internal constructor(
        version: String, latestVersion: Boolean, buildType: BuildType, libraries: MutableList<Library?>?,
        dependencyVersions: MutableMap<String?, String>?, projectProperties: MutableMap<String?, *>?
    ) {
        this.version = version
        this.latestVersion = latestVersion
        this.buildType = buildType
        this.artifactRelease = ArtifactRelease.forVersion(version)
        this.libraries = if (libraries != null) libraries else mutableListOf<Library?>()
        this.dependencyVersions =
            if (dependencyVersions != null) dependencyVersions else mutableMapOf<String?, String?>()
        this.projectProperties = if (projectProperties != null) projectProperties else mutableMapOf<String?, Any?>()
    }

    fun get(): MutableMap<String?, String?> {
        val attributes: MutableMap<String?, String?> = LinkedHashMap<String?, String?>()
        val internal: MutableMap<String?, String?> = LinkedHashMap<String?, String?>()
        addBuildTypeAttribute(attributes)
        addGitHubAttributes(attributes)
        addVersionAttributes(attributes, internal)
        addArtifactAttributes(attributes)
        addUrlJava(attributes)
        addUrlLibraryLinkAttributes(attributes)
        addPropertyAttributes(attributes, internal)
        return attributes
    }

    private fun addBuildTypeAttribute(attributes: MutableMap<String?, String?>) {
        attributes.put("build-type", this.buildType.toIdentifier())
    }

    private fun addGitHubAttributes(attributes: MutableMap<String?, String?>) {
        attributes.put("github-repo", "spring-projects/spring-boot")
        attributes.put("github-ref", determineGitHubRef())
    }

    private fun determineGitHubRef(): String {
        val snapshotIndex: Int = this.version.lastIndexOf(DASH_SNAPSHOT)
        if (snapshotIndex == -1) {
            return "v" + this.version
        }
        if (this.latestVersion) {
            return "main"
        }
        val versionRoot = this.version.substring(0, snapshotIndex)
        val lastDot = versionRoot.lastIndexOf('.')
        return versionRoot.substring(0, lastDot) + ".x"
    }

    private fun addVersionAttributes(attributes: MutableMap<String?, String?>, internal: MutableMap<String?, String?>) {
        this.libraries.forEach(Consumer { library: Library? ->
            val name = "version-" + library!!.getLinkRootName()
            val value: String? = library.getVersion().toString()
            attributes.put(name, value)
        })
        attributes.put("version-native-build-tools", this.projectProperties.get("nativeBuildToolsVersion") as String?)
        attributes.put("version-graal", this.projectProperties.get("graalVersion") as String?)
        attributes.put(
            "version-protobuf-gradle-plugin",
            this.projectProperties.get("protobufGradlePluginVersion") as String?
        )
        addDependencyVersion(attributes, "grpc-api", "io.grpc:grpc-api")
        addDependencyVersion(attributes, "jackson-annotations", "com.fasterxml.jackson.core:jackson-annotations")
        addDependencyVersion(attributes, "jackson-core", "tools.jackson.core:jackson-core")
        addDependencyVersion(attributes, "jackson-databind", "tools.jackson.core:jackson-databind")
        addDependencyVersion(attributes, "jackson-dataformat-xml", "tools.jackson.dataformat:jackson-dataformat-xml")
        addDependencyVersion(attributes, "jackson2-databind", "com.fasterxml.jackson.core:jackson-databind")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-commons")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-couchbase")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-cassandra")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-elasticsearch")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-jdbc")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-jpa")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-mongodb")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-neo4j")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-r2dbc")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-redis")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-rest", "spring-data-rest-core")
        addSpringDataDependencyVersion(attributes, internal, "spring-data-ldap")
        addDependencyVersion(attributes, "pulsar-client-api", "org.apache.pulsar:pulsar-client-api")
    }

    private fun addSpringDataDependencyVersion(
        attributes: MutableMap<String?, String?>, internal: MutableMap<String?, String?>,
        artifactId: String
    ) {
        addSpringDataDependencyVersion(attributes, internal, artifactId, artifactId)
    }

    private fun addSpringDataDependencyVersion(
        attributes: MutableMap<String?, String?>, internal: MutableMap<String?, String?>,
        name: String?, artifactId: String
    ) {
        val groupAndArtifactId = "org.springframework.data:" + artifactId
        addDependencyVersion(attributes, name, groupAndArtifactId)
        val version = getVersion(groupAndArtifactId)
        val majorMinor =
            Arrays.stream<String>(version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).limit(2)
                .collect(
                    Collectors.joining(".")
                )
        val antoraVersion = if (version.endsWith(DASH_SNAPSHOT)) majorMinor + DASH_SNAPSHOT else majorMinor
        internal.put("antoraversion-" + name, antoraVersion)
        internal.put("dotxversion-" + name, majorMinor + ".x")
    }

    private fun addDependencyVersion(
        attributes: MutableMap<String?, String?>,
        name: String?,
        groupAndArtifactId: String?
    ) {
        attributes.put("version-" + name, getVersion(groupAndArtifactId))
    }

    private fun getVersion(groupAndArtifactId: String?): String {
        val version: String = this.dependencyVersions.get(groupAndArtifactId)!!
        Assert.notNull(version, Supplier { "No version found for " + groupAndArtifactId })
        return version
    }

    private fun addArtifactAttributes(attributes: MutableMap<String?, String?>) {
        attributes.put("url-artifact-repository", this.artifactRelease.getDownloadRepo())
        attributes.put("artifact-release-type", this.artifactRelease.getType())
        attributes.put(
            "build-and-artifact-release-type",
            this.buildType.toIdentifier() + "-" + this.artifactRelease.getType()
        )
    }

    private fun addUrlJava(attributes: MutableMap<String?, String?>) {
        attributes.put("url-javase-javadoc", "https://docs.oracle.com/en/java/javase/17/docs/api")
        attributes.put("javadoc-location-java", "{url-javase-javadoc}/java.base")
        attributes.put("javadoc-location-java-beans", "{url-javase-javadoc}/java.desktop")
        attributes.put("javadoc-location-java-sql", "{url-javase-javadoc}/java.sql")
        attributes.put("javadoc-location-javax", "{url-javase-javadoc}/java.base")
        attributes.put("javadoc-location-javax-management", "{url-javase-javadoc}/java.management")
        attributes.put("javadoc-location-javax-net", "{url-javase-javadoc}/java.base")
        attributes.put("javadoc-location-javax-sql", "{url-javase-javadoc}/java.sql")
        attributes.put("javadoc-location-javax-xml", "{url-javase-javadoc}/java.xml")
    }

    private fun addUrlLibraryLinkAttributes(attributes: MutableMap<String?, String?>) {
        val packageAttributes: MutableMap<String?, String?> = LinkedHashMap<String?, String?>()
        this.libraries.forEach(Consumer { library: Library? ->
            library!!.getLinks().forEach { (name: String?, links: MutableList<Library.Link?>?) ->
                links!!.forEach(
                    Consumer { link: Library.Link? ->
                        val linkRootName = if (link!!.rootName != null) link.rootName else library.getLinkRootName()
                        val linkName = "url-" + linkRootName + "-" + name
                        attributes.put(linkName, link.url(library))
                        link.packages
                            .stream()
                            .map<String?> { packageName: String? -> this.packageAttributeName(packageName!!) }
                            .forEach { packageAttributeName: String? ->
                                packageAttributes.put(
                                    packageAttributeName,
                                    "{" + linkName + "}"
                                )
                            }
                    })
            }
        })
        attributes.putAll(packageAttributes)
    }

    private fun packageAttributeName(packageName: String): String {
        return "javadoc-location-" + packageName.replace('.', '-')
    }

    private fun addPropertyAttributes(
        attributes: MutableMap<String?, String?>,
        internal: MutableMap<String?, String?>
    ) {
        val properties: Properties = object : Properties() {
            @Synchronized
            override fun put(key: Any, value: Any): Any? {
                // Put directly because order is important for us
                return attributes.put(key.toString(), resolve(value.toString(), internal))
            }
        }
        try {
            javaClass.getResourceAsStream("antora-asciidoc-attributes.properties").use { `in` ->
                properties.load(`in`)
            }
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }

    private fun resolve(value: String, internal: MutableMap<String?, String?>): String {
        var value = value
        for (entry in internal.entries) {
            value = value.replace("{" + entry.key + "}", entry.value!!)
        }
        return value
    }

    companion object {
        private const val DASH_SNAPSHOT = "-SNAPSHOT"

        private fun dependencyVersionsOf(resolvedBom: ResolvedBom): MutableMap<String?, String> {
            val dependencyVersions: MutableMap<String?, String> = HashMap<String?, String>()
            for (library in resolvedBom.libraries) {
                dependencyVersions.putAll(dependencyVersionsOf(library.managedDependencies))
                for (importedBom in library.importedBoms) {
                    dependencyVersions.putAll(dependencyVersionsOf(importedBom))
                }
            }
            return dependencyVersions
        }

        private fun dependencyVersionsOf(bom: Bom?): MutableMap<String?, String?> {
            val dependencyVersions: MutableMap<String?, String?> = HashMap<String?, String?>()
            if (bom != null) {
                dependencyVersions.putAll(dependencyVersionsOf(bom.managedDependencies))
                dependencyVersions.putAll(dependencyVersionsOf(bom.parent))
                for (importedBom in bom.importedBoms) {
                    dependencyVersions.putAll(dependencyVersionsOf(importedBom))
                }
            }
            return dependencyVersions
        }

        private fun dependencyVersionsOf(managedDependencies: MutableCollection<ResolvedBom.Id>): MutableMap<String?, String?> {
            val dependencyVersions: MutableMap<String?, String?> = HashMap<String?, String?>()
            for (managedDependency in managedDependencies) {
                dependencyVersions.put(
                    managedDependency.groupId + ":" + managedDependency.artifactId,
                    managedDependency.version
                )
            }
            return dependencyVersions
        }
    }
}
