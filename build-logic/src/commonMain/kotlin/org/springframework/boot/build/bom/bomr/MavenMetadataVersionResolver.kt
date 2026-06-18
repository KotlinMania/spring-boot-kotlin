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

import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.internal.artifacts.repositories.AuthenticationSupportedInternal
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import org.springframework.boot.build.xml.XmlDocument
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import org.w3c.dom.NodeList
import java.util.SortedSet
import java.util.function.Supplier
import java.util.stream.Collectors
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * A [VersionResolver] that examines `maven-metadata.xml` to determine the
 * available versions.
 * 
 * @author Andy Wilkinson
 */
internal class MavenMetadataVersionResolver(
    private val rest: RestTemplate,
    private val repositories: MutableCollection<MavenArtifactRepository>
) : VersionResolver {
    constructor(repositories: MutableCollection<MavenArtifactRepository>) : this(
        RestTemplate(
            mutableListOf<HttpMessageConverter<*>?>(
                StringHttpMessageConverter()
            )
        ), repositories
    )

    override fun resolveVersions(groupId: String, artifactId: String?): SortedSet<DependencyVersion?> {
        val versions: MutableSet<String?> = HashSet<String?>()
        for (repository in this.repositories) {
            versions.addAll(resolveVersions(groupId, artifactId, repository))
        }
        return versions.stream().map<DependencyVersion?> { version: String? -> DependencyVersion.parse(version) }
            .collect(
                Collectors.toCollection(Supplier { TreeSet() })
            )
    }

    private fun resolveVersions(
        groupId: String,
        artifactId: String?,
        repository: MavenArtifactRepository
    ): MutableSet<String?> {
        val versions: MutableSet<String?> = HashSet<String?>()
        val url = UriComponentsBuilder.fromUri(repository.getUrl())
            .pathSegment(groupId.replace('.', '/'), artifactId!!, "maven-metadata.xml")
            .build()
            .toUri()
        try {
            val headers = HttpHeaders()
            val credentials = credentialsOf(repository)
            val username = if (credentials != null) credentials.getUsername() else null
            if (username != null) {
                headers.setBasicAuth(username, credentials!!.getPassword()!!)
            }
            val request: HttpEntity<Void?> = HttpEntity<Void?>(headers)
            val metadata: String? =
                this.rest.exchange<String?>(url, HttpMethod.GET, request, String::class.java).getBody()
            val metadataDocument = XmlDocument.parseContent(metadata)
            val versionNodes = XPathFactory.newInstance()
                .newXPath()
                .evaluate("/metadata/versioning/versions/version", metadataDocument, XPathConstants.NODESET) as NodeList
            for (i in 0..<versionNodes.getLength()) {
                versions.add(versionNodes.item(i).getTextContent())
            }
        } catch (ex: HttpClientErrorException) {
            if (ex.getStatusCode() !== HttpStatus.NOT_FOUND) {
                System.err.println(
                    ("Failed to download maven-metadata.xml for " + groupId + ":" + artifactId + " from "
                            + url + ": " + ex.message)
                )
            }
        } catch (ex: Exception) {
            System.err.println(
                ("Failed to resolve versions for module " + groupId + ":" + artifactId + " in repository "
                        + repository + ": " + ex.message)
            )
        }
        return versions
    }

    /**
     * Retrieves the configured credentials of the given `repository`. We cannot use
     * [MavenArtifactRepository.getCredentials] as, if the repository has no
     * credentials, it has the unwanted side-effect of assigning an empty set of username
     * and password credentials to the repository which may cause subsequent "Username
     * must not be null!" failures.
     * @param repository the repository that is the source of the credentials
     * @return the configured password credentials or `null`
     */
    private fun credentialsOf(repository: MavenArtifactRepository): PasswordCredentials? {
        val credentials = (repository as AuthenticationSupportedInternal).getConfiguredCredentials().getOrNull()
        if (credentials != null) {
            if (credentials is PasswordCredentials) {
                return credentials
            }
            throw IllegalStateException(
                "Repository '%s (%s)' has credentials '%s' that are not PasswordCredentials"
                    .formatted(repository.getName(), repository.getUrl(), credentials)
            )
        }
        return null
    }
}
