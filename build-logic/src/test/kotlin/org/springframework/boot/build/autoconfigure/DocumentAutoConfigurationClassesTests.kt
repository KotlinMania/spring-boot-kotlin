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
package org.springframework.boot.build.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Properties
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * Tests for [DocumentAutoConfigurationClasses].
 * 
 * @author Andy Wilkinson
 */
internal class DocumentAutoConfigurationClassesTests {
    @TempDir
    private val temp: File? = null

    @Test
    @kotlin.Throws(IOException::class)
    fun classesAreDocumented() {
        val output: File = documentAutoConfigurationClasses(Consumer { metadataDir ->
            writeAutoConfigurationMetadata(
                "spring-boot-one", List.of(
                    "org.springframework.boot.one.AAutoConfiguration",
                    "org.springframework.boot.one.BAutoConfiguration"
                ), metadataDir
            )
            writeAutoConfigurationMetadata(
                "spring-boot-two", List.of(
                    "org.springframework.boot.two.CAutoConfiguration",
                    "org.springframework.boot.two.DAutoConfiguration"
                ), metadataDir
            )
        })
        assertThat(output).isNotEmptyDirectory()
        assertThat(output.listFiles()).extracting(File::getName)
            .containsExactlyInAnyOrder("spring-boot-one.adoc", "spring-boot-two.adoc", "nav.adoc")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun whenMetadataIsRemovedThenOutputForThatMetadataIsNoLongerPresent() {
        documentAutoConfigurationClasses(Consumer { metadataDir ->
            writeAutoConfigurationMetadata(
                "spring-boot-one", List.of(
                    "org.springframework.boot.one.AAutoConfiguration",
                    "org.springframework.boot.one.BAutoConfiguration"
                ), metadataDir
            )
            writeAutoConfigurationMetadata(
                "spring-boot-two", List.of(
                    "org.springframework.boot.two.CAutoConfiguration",
                    "org.springframework.boot.two.DAutoConfiguration"
                ), metadataDir
            )
        })
        val output: File = documentAutoConfigurationClasses(
            Consumer { metadataDir -> assertThat(File(metadataDir, "spring-boot-two.properties").delete()).isTrue() })
        assertThat(output).isNotEmptyDirectory()
        assertThat(output.listFiles()).extracting(File::getName)
            .containsExactlyInAnyOrder("spring-boot-one.adoc", "nav.adoc")
    }

    @kotlin.Throws(IOException::class)
    private fun documentAutoConfigurationClasses(metadataDir: Consumer<File?>): File {
        val project: Project = ProjectBuilder.builder().build()
        val task: DocumentAutoConfigurationClasses = project.getTasks()
            .register("documentAutoConfigurationClasses", DocumentAutoConfigurationClasses::class.java)
            .get()
        val output: File = File(this.temp, "output")
        val input: File = File(this.temp, "input")
        input.mkdirs()
        metadataDir.accept(input)
        val autoConfiguration: ConfigurableFileCollection = project.files()
        Stream.of(input.listFiles()).forEach(autoConfiguration::from)
        task.getOutputDir().set(output)
        task.setAutoConfiguration(autoConfiguration)
        task.documentAutoConfigurationClasses()
        return output
    }

    private fun writeAutoConfigurationMetadata(module: String?, classes: List<String?>?, outputDir: File?) {
        val metadata: File = File(outputDir, module.toString() + ".properties")
        val properties: Properties = Properties()
        properties.setProperty("autoConfigurationClassNames", String.join(",", classes))
        properties.setProperty("module", module)
        try {
            FileOutputStream(metadata).use { out ->
                properties.store(out, null)
            }
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }
}
