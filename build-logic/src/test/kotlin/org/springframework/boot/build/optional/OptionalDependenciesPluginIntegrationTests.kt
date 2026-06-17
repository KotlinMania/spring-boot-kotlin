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
package org.springframework.boot.build.optional

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

/**
 * Integration tests for [OptionalDependenciesPlugin].
 * 
 * @author Andy Wilkinson
 */
internal class OptionalDependenciesPluginIntegrationTests {
    private var projectDir: File? = null

    private var buildFile: File? = null

    @BeforeEach
    fun setup(@TempDir projectDir: File?) {
        this.projectDir = projectDir
        this.buildFile = File(this.projectDir, "build.gradle")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun optionalConfigurationIsCreated() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins { id 'org.springframework.boot.optional-dependencies' }")
            out.println("task printConfigurations {")
            out.println("    doLast {")
            out.println("        configurations.all { println it.name }")
            out.println("    }")
            out.println("}")
        }
        val buildResult: BuildResult = runGradle("printConfigurations")
        assertThat(buildResult.getOutput()).contains(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME)
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun optionalDependenciesAreAddedToMainSourceSetsCompileClasspath() {
        optionalDependenciesAreAddedToSourceSetClasspath("main", "compileClasspath")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun optionalDependenciesAreAddedToMainSourceSetsRuntimeClasspath() {
        optionalDependenciesAreAddedToSourceSetClasspath("main", "runtimeClasspath")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun optionalDependenciesAreAddedToTestSourceSetsCompileClasspath() {
        optionalDependenciesAreAddedToSourceSetClasspath("test", "compileClasspath")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun optionalDependenciesAreAddedToTestSourceSetsRuntimeClasspath() {
        optionalDependenciesAreAddedToSourceSetClasspath("test", "runtimeClasspath")
    }

    @kotlin.Throws(IOException::class)
    private fun optionalDependenciesAreAddedToSourceSetClasspath(sourceSet: String?, classpath: String?) {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'org.springframework.boot.optional-dependencies'")
            out.println("    id 'java'")
            out.println("}")
            out.println("repositories {")
            out.println("    mavenCentral()")
            out.println("}")
            out.println("dependencies {")
            out.println("    optional 'org.springframework:spring-jcl:5.1.2.RELEASE'")
            out.println("}")
            out.println("task printClasspath {")
            out.println("    doLast {")
            out.println("        println sourceSets." + sourceSet + "." + classpath + ".files")
            out.println("    }")
            out.println("}")
        }
        val buildResult: BuildResult = runGradle("printClasspath")
        assertThat(buildResult.getOutput()).contains("spring-jcl")
    }

    private fun runGradle(vararg args: String?): BuildResult {
        return GradleRunner.create().withProjectDir(this.projectDir).withArguments(args).withPluginClasspath().build()
    }
}
