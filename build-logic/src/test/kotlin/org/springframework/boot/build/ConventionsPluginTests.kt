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

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Integration tests for [ConventionsPlugin].
 * 
 * @author Christoph Dreis
 */
internal class ConventionsPluginTests {
    private var projectDir: File? = null

    private var buildFile: File? = null

    @BeforeEach
    @kotlin.Throws(IOException::class)
    fun setup(@TempDir projectDir: File) {
        this.projectDir = projectDir
        this.buildFile = File(this.projectDir, "build.gradle")
        val settingsFile: File = File(this.projectDir, "settings.gradle")
        PrintWriter(FileWriter(settingsFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'com.gradle.develocity'")
            out.println("}")
            out.println("include ':platform:spring-boot-internal-dependencies'")
        }
        val internalDependencies: File = File(
            this.projectDir,
            "platform/spring-boot-internal-dependencies/build.gradle"
        )
        internalDependencies.getParentFile().mkdirs()
        PrintWriter(FileWriter(internalDependencies)).use { out ->
            out.println("plugins {")
            out.println("    id 'java-platform'")
            out.println("}")
        }
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun jarIncludesLegalFiles() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'java'")
            out.println("    id 'org.springframework.boot.conventions'")
            out.println("}")
            out.println("version = '1.2.3'")
            out.println("java {")
            out.println("    sourceCompatibility = '17'")
            out.println("}")
            out.println("description 'Test project for manifest customization'")
            out.println("jar.archiveFileName = 'test.jar'")
        }
        runGradle("jar")
        val file: File = File(this.projectDir, "/build/libs/test.jar")
        assertThat(file).exists()
        JarFile(file).use { jar ->
            assertThatLicenseIsPresent(jar)
            assertThatNoticeIsPresent(jar)
            val mainAttributes: Attributes = jar.getManifest().getMainAttributes()
            assertThat(mainAttributes.getValue("Implementation-Title"))
                .isEqualTo("Test project for manifest customization")
            assertThat(mainAttributes.getValue("Automatic-Module-Name"))
                .isEqualTo(this.projectDir.getName().replace("-", "."))
            assertThat(mainAttributes.getValue("Implementation-Version")).isEqualTo("1.2.3")
            assertThat(mainAttributes.getValue("Built-By")).isEqualTo("Spring")
            assertThat(mainAttributes.getValue("Build-Jdk-Spec")).isEqualTo("17")
        }
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun sourceJarIsBuilt() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'java'")
            out.println("    id 'maven-publish'")
            out.println("    id 'org.springframework.boot.conventions'")
            out.println("}")
            out.println("version = '1.2.3'")
            out.println("java {")
            out.println("    sourceCompatibility = '17'")
            out.println("}")
            out.println("description 'Test'")
        }
        runGradle("assemble")
        val file: File = File(this.projectDir, "/build/libs/" + this.projectDir.getName() + "-1.2.3-sources.jar")
        assertThat(file).exists()
        JarFile(file).use { jar ->
            assertThatLicenseIsPresent(jar)
            assertThatNoticeIsPresent(jar)
            val mainAttributes: Attributes = jar.getManifest().getMainAttributes()
            assertThat(mainAttributes.getValue("Implementation-Title"))
                .isEqualTo("Source for " + this.projectDir.getName())
            assertThat(mainAttributes.getValue("Automatic-Module-Name"))
                .isEqualTo(this.projectDir.getName().replace("-", "."))
            assertThat(mainAttributes.getValue("Implementation-Version")).isEqualTo("1.2.3")
            assertThat(mainAttributes.getValue("Built-By")).isEqualTo("Spring")
            assertThat(mainAttributes.getValue("Build-Jdk-Spec")).isEqualTo("17")
        }
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun javadocJarIsBuilt() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'java'")
            out.println("    id 'maven-publish'")
            out.println("    id 'org.springframework.boot.conventions'")
            out.println("}")
            out.println("version = '1.2.3'")
            out.println("java {")
            out.println("    sourceCompatibility = '17'")
            out.println("}")
            out.println("description 'Test'")
        }
        runGradle("assemble")
        val file: File = File(this.projectDir, "/build/libs/" + this.projectDir.getName() + "-1.2.3-javadoc.jar")
        assertThat(file).exists()
        JarFile(file).use { jar ->
            assertThatLicenseIsPresent(jar)
            assertThatNoticeIsPresent(jar)
            val mainAttributes: Attributes = jar.getManifest().getMainAttributes()
            assertThat(mainAttributes.getValue("Implementation-Title"))
                .isEqualTo("Javadoc for " + this.projectDir.getName())
            assertThat(mainAttributes.getValue("Automatic-Module-Name"))
                .isEqualTo(this.projectDir.getName().replace("-", "."))
            assertThat(mainAttributes.getValue("Implementation-Version")).isEqualTo("1.2.3")
            assertThat(mainAttributes.getValue("Built-By")).isEqualTo("Spring")
            assertThat(mainAttributes.getValue("Build-Jdk-Spec")).isEqualTo("17")
        }
    }

    @kotlin.Throws(IOException::class)
    private fun assertThatLicenseIsPresent(jar: JarFile) {
        val license: JarEntry? = jar.getJarEntry("META-INF/LICENSE.txt")
        assertThat(license).isNotNull()
        val licenseContent: String? = FileCopyUtils.copyToString(InputStreamReader(jar.getInputStream(license)))
        assertThat(licenseContent).isEqualTo(
            Files.readString(
                Path.of(
                    "src", "main", "resources", "org",
                    "springframework", "boot", "build", "legal", "LICENSE.txt"
                )
            )
        )
    }

    @kotlin.Throws(IOException::class)
    private fun assertThatNoticeIsPresent(jar: JarFile) {
        val notice: JarEntry? = jar.getJarEntry("META-INF/NOTICE.txt")
        assertThat(notice).isNotNull()
        val noticeContent: String? = FileCopyUtils.copyToString(InputStreamReader(jar.getInputStream(notice)))
        // Test that variables were replaced
        assertThat(noticeContent).doesNotContain("\${")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun testRetryIsConfiguredWithThreeRetriesOnCI() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'java'")
            out.println("    id 'org.springframework.boot.conventions'")
            out.println("}")
            out.println("description 'Test'")
            out.println("task retryConfig {")
            out.println("    doLast {")
            out.println("        test.develocity.testRetry {")
            out.println("            println \"maxRetries: \${maxRetries.get()}\"")
            out.println("            println \"failOnPassedAfterRetry: \${failOnPassedAfterRetry.get()}\"")
            out.println("        }")
            out.println("    }")
            out.println("}")
        }
        assertThat(runGradle(Collections.singletonMap("CI", "true"), "retryConfig", "--stacktrace").getOutput())
            .contains("maxRetries: 3")
            .contains("failOnPassedAfterRetry: false")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun testRetryIsConfiguredWithZeroRetriesLocally() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'java'")
            out.println("    id 'org.springframework.boot.conventions'")
            out.println("}")
            out.println("description 'Test'")
            out.println("task retryConfig {")
            out.println("    doLast {")
            out.println("        test.develocity.testRetry {")
            out.println("            println \"maxRetries: \${maxRetries.get()}\"")
            out.println("            println \"failOnPassedAfterRetry: \${failOnPassedAfterRetry.get()}\"")
            out.println("        }")
            out.println("    }")
            out.println("}")
        }
        assertThat(runGradle(Collections.singletonMap("CI", "local"), "retryConfig", "--stacktrace").getOutput())
            .contains("maxRetries: 0")
            .contains("failOnPassedAfterRetry: false")
    }

    private fun runGradle(vararg args: String?): BuildResult? {
        return runGradle(Collections.emptyMap(), args)
    }

    private fun runGradle(environment: Map<String?, String?>?, vararg args: String?): BuildResult {
        return GradleRunner.create()
            .withProjectDir(this.projectDir)
            .withEnvironment(environment)
            .withArguments(args)
            .withPluginClasspath()
            .build()
    }
}
