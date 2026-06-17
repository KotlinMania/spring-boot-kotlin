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
package org.springframework.boot.build

import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.util.*
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import javax.inject.Inject

/**
 * Extension to add `springRepositoryTransformers` utility methods.
 * 
 * @author Phillip Webb
 */
class RepositoryTransformersExtension @Inject constructor(private val project: Project) {
    fun ant(): Transformer<String?, String?> {
        return Transformer { line: String? -> this.transformAnt(line!!) }
    }

    private fun transformAnt(line: String): String {
        if (line.contains(REPOSITORIES_MARKER)) {
            return transform(line, BiFunction { repository: MavenArtifactRepository?, indent: String? ->
                val name = repository!!.name
                val url = repository.getUrl()
                "%s<ibiblio name=\"%s\" m2compatible=\"true\" root=\"%s\" />".format(indent, name, url)
            })
        }
        if (line.contains(CREDENTIALS_MARKER)) {
            val hostCredentials: MutableMap<String?, MavenCredential?> = LinkedHashMap<String?, MavenCredential?>()
            this.springRepositories.forEach(Consumer { repository: MavenArtifactRepository? ->
                if (repository!!.name.startsWith("spring-commercial-")) {
                    val host = repository.getUrl().getHost()
                    hostCredentials.put(
                        host,
                        MavenCredential("\${env.COMMERCIAL_REPO_USERNAME}", "\${env.COMMERCIAL_REPO_PASSWORD}")
                    )
                }
            })
            return transform<MutableMap.MutableEntry<String?, MavenCredential?>?>(
                line,
                hostCredentials.entries,
                BiFunction { entry: MutableMap.MutableEntry<String?, MavenCredential?>?, indent: String? ->
                    "%s<credentials host=\"%s\" realm=\"Artifactory Realm\" username=\"%s\" passwd=\"%s\" />%n"
                        .format(indent, entry!!.key, entry.value!!.username, entry.value!!.password)
                })
        }
        return line
    }

    private fun transformMavenRepositories(line: String, pluginRepository: Boolean): String {
        return transform(
            line,
            BiFunction { repository: MavenArtifactRepository?, indent: String? ->
                mavenRepositoryXml(
                    indent,
                    repository!!,
                    pluginRepository
                )
            })
    }

    private fun mavenRepositoryXml(
        indent: String?,
        repository: MavenArtifactRepository,
        pluginRepository: Boolean
    ): String {
        val rootTag = if (pluginRepository) "pluginRepository" else "repository"
        val snapshots = repository.name.endsWith("-snapshot")
        val xml = StringBuilder()
        xml.append("%s<%s>%n".format(indent, rootTag))
        xml.append("%s\t<id>%s</id>%n".format(indent, repository.name))
        xml.append("%s\t<url>%s</url>%n".format(indent, repository.getUrl()))
        xml.append("%s\t<releases>%n".format(indent))
        xml.append("%s\t\t<enabled>%s</enabled>%n".format(indent, !snapshots))
        xml.append("%s\t</releases>%n".format(indent))
        xml.append("%s\t<snapshots>%n".format(indent))
        xml.append("%s\t\t<enabled>%s</enabled>%n".format(indent, snapshots))
        xml.append("%s\t</snapshots>%n".format(indent))
        xml.append("%s</%s>".format(indent, rootTag))
        return xml.toString()
    }

    private fun transform(line: String, generator: BiFunction<MavenArtifactRepository?, String?, String?>): String {
        return transform<MavenArtifactRepository?>(
            line,
            this.springRepositories, generator
        )
    }

    private fun <T> transform(
        line: String,
        iterable: Iterable,
        generator: BiFunction<T?, String?, String?>
    ): String {
        val result = StringBuilder()
        val indent = getIndent(line)
        iterable.forEach(Consumer { item: T? ->
            val fragment = generator.apply(item, indent)
            if (fragment != null) {
                result.append(if (!result.isEmpty()) "\n" else "")
                result.append(fragment)
            }
        })
        return result.toString()
    }

    private val springRepositories: MutableList<MavenArtifactRepository?>
        get() {
            val springRepositories: MutableList<MavenArtifactRepository?> =
                ArrayList<MavenArtifactRepository?>(
                    this.project.getRepositories()
                        .withType<MavenArtifactRepository>(MavenArtifactRepository::class.java)
                        .stream()
                        .filter { repository: MavenArtifactRepository? ->
                            this.isSpringRepository(
                                repository!!
                            )
                        }
                        .toList())
            val bySnapshots: Function<MavenArtifactRepository?, Boolean?> =
                Function { repository: MavenArtifactRepository? ->
                    repository!!.name
                        .contains("snapshot")
                }
            val byName: Function<MavenArtifactRepository?, String?> =
                Function { obj: MavenArtifactRepository? -> obj!!.name }
            Collections.sort<MavenArtifactRepository?>(
                springRepositories,
                Comparator.comparing<MavenArtifactRepository?, Boolean?>(
                    bySnapshots
                ).thenComparing<String?>(byName)
            )
            return springRepositories
        }

    private fun isSpringRepository(repository: MavenArtifactRepository): Boolean {
        return (repository.name.startsWith("spring-"))
    }

    private fun getIndent(line: String): String {
        return line.substring(0, line.length - line.trimStart().length)
    }

    @JvmRecord
    data class MavenCredential(val username: String?, val password: String?)

    companion object {
        private const val CREDENTIALS_MARKER = "{spring.mavenCredentials}"

        private const val REPOSITORIES_MARKER = "{spring.mavenRepositories}"

        fun apply(project: Project) {
            project.getExtensions().create<RepositoryTransformersExtension>(
                "springRepositoryTransformers",
                RepositoryTransformersExtension::class.java,
                project
            )
        }
    }
}
