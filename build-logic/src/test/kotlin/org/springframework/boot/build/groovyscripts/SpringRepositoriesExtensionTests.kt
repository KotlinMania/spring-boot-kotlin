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
package org.springframework.boot.build.groovyscripts

import groovy.lang.Closure
import groovy.lang.GroovyClassLoader
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.function.UnaryOperator

/**
 * Tests for `SpringRepositorySupport.groovy`.
 * 
 * @author Phillip Webb
 */
internal class SpringRepositoriesExtensionTests {
    private val repositories: List<MavenArtifactRepository?> = ArrayList()

    private val contents: List<RepositoryContentDescriptor?> = ArrayList()

    private val credentials: List<PasswordCredentials?> = ArrayList()

    private val mavenContent: List<MavenRepositoryContentDescriptor?> = ArrayList()

    @Test
    fun mavenRepositoriesWhenNotCommercialSnapshot() {
        val extension = createExtension("0.0.0-SNAPSHOT", "oss")
        extension.mavenRepositories()
        assertThat(this.repositories).hasSize(1)
        verify(this.repositories.get(0)).setName("spring-oss-snapshot")
        verify(this.repositories.get(0)).setUrl("https://repo.spring.io/snapshot")
        verify(this.mavenContent.get(0)).snapshotsOnly()
    }

    @Test
    fun mavenRepositoriesWhenCommercialSnapshot() {
        val extension = createExtension("0.0.0-SNAPSHOT", "commercial")
        extension.mavenRepositories()
        assertThat(this.repositories).hasSize(3)
        verify(this.repositories.get(0)).setName("spring-commercial-release")
        verify(this.repositories.get(0))
            .setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-prod-local")
        verify(this.mavenContent.get(0)).releasesOnly()
        verify(this.repositories.get(1)).setName("spring-commercial-snapshot")
        verify(this.repositories.get(1)).setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-dev-local")
        verify(this.mavenContent.get(1)).snapshotsOnly()
        verify(this.repositories.get(2)).setName("spring-oss-snapshot")
        verify(this.repositories.get(2)).setUrl("https://repo.spring.io/snapshot")
        verify(this.mavenContent.get(2)).snapshotsOnly()
    }

    @Test
    fun mavenRepositoriesWhenNotCommercialMilestone() {
        val extension = createExtension("0.0.0-M1", "oss")
        extension.mavenRepositories()
        assertThat(this.repositories).isEmpty()
    }

    @Test
    fun mavenRepositoriesWhenCommercialMilestone() {
        val extension = createExtension("0.0.0-M1", "commercial")
        extension.mavenRepositories()
        assertThat(this.repositories).hasSize(1)
        verify(this.repositories.get(0)).setName("spring-commercial-release")
        verify(this.repositories.get(0))
            .setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-prod-local")
        verify(this.mavenContent.get(0)).releasesOnly()
    }

    @Test
    fun mavenRepositoriesWhenNotCommercialRelease() {
        val extension = createExtension("0.0.1", "oss")
        extension.mavenRepositories()
        assertThat(this.repositories).isEmpty()
    }

    @Test
    fun mavenRepositoriesWhenCommercialRelease() {
        val extension = createExtension("0.0.1", "commercial")
        extension.mavenRepositories()
        assertThat(this.repositories).hasSize(1)
        verify(this.repositories.get(0)).setName("spring-commercial-release")
        verify(this.repositories.get(0))
            .setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-prod-local")
        verify(this.mavenContent.get(0)).releasesOnly()
    }

    @Test
    fun mavenRepositoriesWhenConditionMatches() {
        val extension = createExtension("0.0.0-SNAPSHOT", "oss")
        extension.mavenRepositoriesFor("1.2.3-SNAPSHOT")
        assertThat(this.repositories).hasSize(1)
    }

    @Test
    fun mavenRepositoriesWhenConditionDoesNotMatch() {
        val extension = createExtension("0.0.0-SNAPSHOT", "oss")
        extension.mavenRepositoriesFor("1.2.3")
        assertThat(this.repositories).isEmpty()
    }

    @Test
    fun mavenRepositoriesExcludingBootGroup() {
        val extension = createExtension("0.0.0-SNAPSHOT", "oss")
        extension.mavenRepositoriesExcludingBootGroup()
        assertThat(this.contents).hasSize(1)
        verify(this.contents.get(0)).excludeGroup("org.springframework.boot")
    }

    @Test
    fun mavenRepositoriesWithRepositorySpecificEnvironmentVariables() {
        val environment: Map<String?, String?> = HashMap()
        environment.put("COMMERCIAL_RELEASE_REPO_URL", "curl")
        environment.put("COMMERCIAL_RELEASE_REPO_USERNAME", "cuser")
        environment.put("COMMERCIAL_RELEASE_REPO_PASSWORD", "cpass")
        environment.put("COMMERCIAL_SNAPSHOT_REPO_URL", "surl")
        environment.put("COMMERCIAL_SNAPSHOT_REPO_USERNAME", "suser")
        environment.put("COMMERCIAL_SNAPSHOT_REPO_PASSWORD", "spass")
        val extension = createExtension("0.0.0-SNAPSHOT", "commercial", environment::get)
        extension.mavenRepositories()
        assertThat(this.repositories).hasSize(3)
        verify(this.repositories.get(0)).setUrl("curl")
        verify(this.repositories.get(1)).setUrl("surl")
        assertThat(this.credentials).hasSize(2)
        verify(this.credentials.get(0)).setUsername("cuser")
        verify(this.credentials.get(0)).setPassword("cpass")
        verify(this.credentials.get(1)).setUsername("suser")
        verify(this.credentials.get(1)).setPassword("spass")
    }

    @Test
    fun mavenRepositoriesWhenRepositoryEnvironmentVariables() {
        val environment: Map<String?, String?> = HashMap()
        environment.put("COMMERCIAL_REPO_URL", "url")
        environment.put("COMMERCIAL_REPO_USERNAME", "user")
        environment.put("COMMERCIAL_REPO_PASSWORD", "pass")
        val extension = createExtension("0.0.0-SNAPSHOT", "commercial", environment::get)
        extension.mavenRepositories()
        assertThat(this.repositories).hasSize(3)
        verify(this.repositories.get(0)).setUrl("url")
        verify(this.repositories.get(1)).setUrl("url")
        assertThat(this.credentials).hasSize(2)
        verify(this.credentials.get(0)).setUsername("user")
        verify(this.credentials.get(0)).setPassword("pass")
        verify(this.credentials.get(1)).setUsername("user")
        verify(this.credentials.get(1)).setPassword("pass")
    }

    private fun createExtension(version: String?, buildType: String?): SpringRepositoriesExtension {
        return createExtension(version, buildType, UnaryOperator { name -> null })
    }

    @SuppressWarnings(["unchecked", "unchecked"])
    private fun createExtension(
        version: String?, buildType: String?,
        environment: UnaryOperator<String?>?
    ): SpringRepositoriesExtension {
        val repositoryHandler: RepositoryHandler = mock(RepositoryHandler::class.java)
        given(repositoryHandler.maven(any(Closure::class.java))).willAnswer({ invocation: InvocationOnMock ->
            this.mavenClosure(
                invocation
            )
        })
        return org.springframework.boot.build.groovyscripts.SpringRepositoriesExtensionTests.SpringRepositoriesExtension.Companion.get(
            repositoryHandler,
            version,
            buildType,
            environment
        )
    }

    @SuppressWarnings(["unchecked", "unchecked"])
    private fun mavenClosure(invocation: InvocationOnMock): Object? {
        val repository: MavenArtifactRepository? = mock(MavenArtifactRepository::class.java)
        willAnswer({ invocation: InvocationOnMock -> this.contentAction(invocation) }).given(repository)
            .content(any(Action::class.java))
        willAnswer({ invocation: InvocationOnMock -> this.credentialsAction(invocation) }).given(repository)
            .credentials(any(Action::class.java))
        willAnswer({ invocation: InvocationOnMock -> this.mavenContentAction(invocation) }).given(repository)
            .mavenContent(any(Action::class.java))
        val closure: Closure<MavenArtifactRepository?> = invocation.getArgument(0)
        closure.call(repository)
        this.repositories.add(repository)
        return null
    }

    private fun contentAction(invocation: InvocationOnMock): Object? {
        val content: RepositoryContentDescriptor? = mock(RepositoryContentDescriptor::class.java)
        val action: Action<RepositoryContentDescriptor?> = invocation.getArgument(0)
        action.execute(content)
        this.contents.add(content)
        return null
    }

    private fun credentialsAction(invocation: InvocationOnMock): Object? {
        val credentials: PasswordCredentials? = mock(PasswordCredentials::class.java)
        val action: Action<PasswordCredentials?> = invocation.getArgument(0)
        action.execute(credentials)
        this.credentials.add(credentials)
        return null
    }

    private fun mavenContentAction(invocation: InvocationOnMock): Object? {
        val mavenContent: MavenRepositoryContentDescriptor? = mock(MavenRepositoryContentDescriptor::class.java)
        val action: Action<MavenRepositoryContentDescriptor?> = invocation.getArgument(0)
        action.execute(mavenContent)
        this.mavenContent.add(mavenContent)
        return null
    }

    internal interface SpringRepositoriesExtension {
        fun mavenRepositories()

        fun mavenRepositoriesFor(version: Object?)

        fun mavenRepositoriesExcludingBootGroup()

        companion object {
            fun get(
                repositoryHandler: RepositoryHandler?, version: String?, buildType: String?,
                environment: UnaryOperator<String?>?
            ): SpringRepositoriesExtension {
                try {
                    val extensionClass: Class<*> =
                        org.springframework.boot.build.groovyscripts.SpringRepositoriesExtensionTests.Companion.supportClass.getClassLoader()
                            .loadClass("SpringRepositoriesExtension")
                    val extension: Object = extensionClass
                        .getDeclaredConstructor(
                            Object::class.java,
                            Object::class.java,
                            Object::class.java,
                            Object::class.java
                        )
                        .newInstance(repositoryHandler, version, buildType, environment)
                    return Proxy.newProxyInstance(
                        org.springframework.boot.build.groovyscripts.SpringRepositoriesExtensionTests::class.java.getClassLoader(),
                        arrayOf<Class<*>>(org.springframework.boot.build.groovyscripts.SpringRepositoriesExtensionTests.SpringRepositoriesExtension::class.java),
                        { instance, method, args ->
                            val params: Array<Class<*>?> = arrayOfNulls<Class<*>>(if (args != null) args.length else 0)
                            Arrays.fill(params, Object::class.java)
                            val groovyMethod: Method = extension.getClass().getDeclaredMethod(method.getName(), params)
                            groovyMethod.invoke(extension, args)
                        }) as SpringRepositoriesExtension
                } catch (ex: Exception) {
                    throw RuntimeException(ex)
                }
            }
        }
    }

    companion object {
        private var groovyClassLoader: GroovyClassLoader? = null

        private var supportClass: Class<*>? = null

        @BeforeAll
        @kotlin.Throws(Exception::class)
        fun loadGroovyClass() {
            org.springframework.boot.build.groovyscripts.SpringRepositoriesExtensionTests.Companion.groovyClassLoader =
                GroovyClassLoader(org.springframework.boot.build.groovyscripts.SpringRepositoriesExtensionTests::class.java.getClassLoader())
            org.springframework.boot.build.groovyscripts.SpringRepositoriesExtensionTests.Companion.supportClass =
                org.springframework.boot.build.groovyscripts.SpringRepositoriesExtensionTests.Companion.groovyClassLoader.parseClass(
                    File("SpringRepositorySupport.groovy")
                )
        }

        @AfterAll
        @kotlin.Throws(Exception::class)
        fun cleanup() {
            org.springframework.boot.build.groovyscripts.SpringRepositoriesExtensionTests.Companion.groovyClassLoader.close()
        }
    }
}
