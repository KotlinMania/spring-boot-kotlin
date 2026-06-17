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
package org.springframework.boot.build.testing

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.io.StringReader
import java.util.function.Consumer

/**
 * Integrations tests for [TestFailuresPlugin].
 * 
 * @author Andy Wilkinson
 */
internal class TestFailuresPluginIntegrationTests {
    private var projectDir: File? = null

    @BeforeEach
    fun setup(@TempDir projectDir: File?) {
        this.projectDir = projectDir
    }

    @Test
    fun singleProject() {
        createProject(this.projectDir)
        val result: BuildResult = GradleRunner.create()
            .withDebug(true)
            .withProjectDir(this.projectDir)
            .withArguments("build")
            .withPluginClasspath()
            .buildAndFail()
        assertThat(readLines(result.getOutput())).containsSequence(
            "Found test failures in 1 test task:", "", ":test",
            "    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
            "    example.MoreTests > bad()", "    example.MoreTests > fail()"
        )
    }

    @Test
    fun multiProject() {
        createMultiProjectBuild()
        val result: BuildResult = GradleRunner.create()
            .withDebug(true)
            .withProjectDir(this.projectDir)
            .withArguments("build")
            .withPluginClasspath()
            .buildAndFail()
        assertThat(readLines(result.getOutput())).containsSequence(
            "Found test failures in 1 test task:", "",
            ":project-one:test", "    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
            "    example.MoreTests > bad()", "    example.MoreTests > fail()"
        )
    }

    @Test
    fun multiProjectContinue() {
        createMultiProjectBuild()
        val result: BuildResult = GradleRunner.create()
            .withDebug(true)
            .withProjectDir(this.projectDir)
            .withArguments("build", "--continue")
            .withPluginClasspath()
            .buildAndFail()
        assertThat(readLines(result.getOutput())).containsSequence(
            "Found test failures in 2 test tasks:", "",
            ":project-one:test", "    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
            "    example.MoreTests > bad()", "    example.MoreTests > fail()", "", ":project-two:test",
            "    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
            "    example.MoreTests > bad()", "    example.MoreTests > fail()"
        )
    }

    @Test
    fun multiProjectParallel() {
        createMultiProjectBuild()
        val result: BuildResult = GradleRunner.create()
            .withDebug(true)
            .withProjectDir(this.projectDir)
            .withArguments("build", "--parallel", "--stacktrace")
            .withPluginClasspath()
            .buildAndFail()
        assertThat(readLines(result.getOutput())).containsSequence(
            "Found test failures in 2 test tasks:", "",
            ":project-one:test", "    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
            "    example.MoreTests > bad()", "    example.MoreTests > fail()", "", ":project-two:test",
            "    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
            "    example.MoreTests > bad()", "    example.MoreTests > fail()"
        )
    }

    private fun createProject(dir: File?) {
        val examplePackage: File = File(dir, "src/test/java/example")
        examplePackage.mkdirs()
        createTestSource("ExampleTests", examplePackage)
        createTestSource("MoreTests", examplePackage)
        createBuildScript(dir)
    }

    private fun createMultiProjectBuild() {
        createProject(File(this.projectDir, "project-one"))
        createProject(File(this.projectDir, "project-two"))
        withPrintWriter(File(this.projectDir, "settings.gradle"), Consumer { writer ->
            writer.println("include 'project-one'")
            writer.println("include 'project-two'")
        })
    }

    private fun createTestSource(name: String?, dir: File?) {
        withPrintWriter(File(dir, name.toString() + ".java"), Consumer { writer ->
            writer.println("package example;")
            writer.println()
            writer.println("import org.junit.jupiter.api.Test;")
            writer.println()
            writer.println("import static org.assertj.core.api.Assertions.assertThat;")
            writer.println()
            writer.println("class " + name + "{")
            writer.println()
            writer.println("	@Test")
            writer.println("	void fail() {")
            writer.println("		assertThat(true).isFalse();")
            writer.println("	}")
            writer.println()
            writer.println("	@Test")
            writer.println("	void bad() {")
            writer.println("		assertThat(5).isLessThan(4);")
            writer.println("	}")
            writer.println()
            writer.println("	@Test")
            writer.println("	void ok() {")
            writer.println("	}")
            writer.println()
            writer.println("}")
        })
    }

    private fun createBuildScript(dir: File?) {
        withPrintWriter(File(dir, "build.gradle"), Consumer { writer ->
            writer.println("plugins {")
            writer.println("	id 'java'")
            writer.println("	id 'org.springframework.boot.test-failures'")
            writer.println("}")
            writer.println()
            writer.println("repositories {")
            writer.println("	mavenCentral()")
            writer.println("}")
            writer.println()
            writer.println("dependencies {")
            writer.println("	testImplementation 'org.junit.jupiter:junit-jupiter:5.6.0'")
            writer.println("	testImplementation 'org.assertj:assertj-core:3.11.1'")
            writer.println("	testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.6.0'")
            writer.println("}")
            writer.println()
            writer.println("test {")
            writer.println("	useJUnitPlatform()")
            writer.println("}")
        })
    }

    private fun withPrintWriter(file: File?, consumer: Consumer<PrintWriter?>) {
        try {
            PrintWriter(FileWriter(file)).use { writer ->
                consumer.accept(writer)
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    private fun readLines(output: String?): List<String?> {
        try {
            BufferedReader(StringReader(output)).use { reader ->
                return reader.lines().toList()
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }
}
