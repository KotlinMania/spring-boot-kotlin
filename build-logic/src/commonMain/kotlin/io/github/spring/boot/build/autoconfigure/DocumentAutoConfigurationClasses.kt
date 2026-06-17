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

import org.gradle.kotlin.dsl.*

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.springframework.util.FileSystemUtils
import org.springframework.util.StringUtils
import java.io.*
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Collectors
import org.gradle.api.file.DirectoryProperty

/**
 * [Task] used to document auto-configuration classes.
 * 
 * @author Andy Wilkinson
 */
abstract class DocumentAutoConfigurationClasses : DefaultTask() {
    var autoConfiguration: FileCollection? = null

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getAutoConfiguration(): FileCollection {
        return this.autoConfiguration!!
    }

    fun setAutoConfiguration(autoConfiguration: FileCollection) {
        this.autoConfiguration = autoConfiguration
    }

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    @Throws(IOException::class)
    fun documentAutoConfigurationClasses() {
        FileSystemUtils.deleteRecursively(this.outputDir.asFile.get())
        val autoConfigurations = load()
        autoConfigurations.forEach(Consumer { autoConfigurationClasses: AutoConfiguration? ->
            this.writeModuleAdoc(
                autoConfigurationClasses!!
            )
        })
        for (metadataFile in this.autoConfiguration!!) {
            val metadata = Properties()
            FileReader(metadataFile).use { reader ->
                metadata.load(reader)
            }
            val autoConfiguration = AutoConfiguration(
                metadata.getProperty("module"), TreeSet<String?>(
                    StringUtils.commaDelimitedListToSet(metadata.getProperty("autoConfigurationClassNames"))
                )
            )
            writeModuleAdoc(autoConfiguration)
        }
        writeNavAdoc(autoConfigurations)
    }

    fun load(): MutableList<AutoConfiguration?> {
        return this.autoConfiguration!!.files
            .stream()
            .map<AutoConfiguration> { metadataFile: File? -> AutoConfiguration.Companion.of(metadataFile) }
            .sorted { a1: AutoConfiguration?, a2: AutoConfiguration? -> a1.module.compareTo(a2.module) }
            .toList()
    }

    fun writeModuleAdoc(autoConfigurationClasses: AutoConfiguration) {
        val outputDir = this.outputDir.asFile.get()
        outputDir.mkdirs()
        try {
            PrintWriter(
                FileWriter(File(outputDir, autoConfigurationClasses.module + ".adoc"))
            ).use { writer ->
                writer.println("[[appendix.auto-configuration-classes.%s]]".format(autoConfigurationClasses.module))
                writer.println("= %s".format(autoConfigurationClasses.module))
                writer.println()
                writer.println(
                    "The following auto-configuration classes are from the `%s` module:"
                        .format(autoConfigurationClasses.module)
                )
                writer.println()
                writer.println("[cols=\"4,1\"]")
                writer.println("|===")
                writer.println("| Configuration Class | Links")
                for (autoConfigurationClass in autoConfigurationClasses.classes) {
                    writer.println()
                    writer.printf(
                        "| {code-spring-boot}/module/%s/src/main/java/%s.java[`%s`]%n",
                        autoConfigurationClasses.module, autoConfigurationClass.path, autoConfigurationClass.name
                    )
                    writer.printf("| xref:api:java/%s.html[javadoc]%n", autoConfigurationClass.path)
                }
                writer.println("|===")
            }
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }

    fun writeNavAdoc(autoConfigurations: MutableList<AutoConfiguration?>) {
        val outputDir = this.outputDir.asFile.get()
        outputDir.mkdirs()
        try {
            PrintWriter(FileWriter(File(outputDir, "nav.adoc"))).use { writer ->
                autoConfigurations.forEach(Consumer { autoConfigurationClasses: AutoConfiguration? ->
                    writer
                        .println(
                            "*** xref:appendix:auto-configuration-classes/%s.adoc[]"
                                .format(autoConfigurationClasses.module)
                        )
                })
            }
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }

    class AutoConfiguration(val module: String, classNames: MutableSet<String?>) {
        val classes: SortedSet<AutoConfigurationClass>

        init {
            this.classes = classNames.stream().map<AutoConfigurationClass> { className: String? ->
                val path = className!!.replace('.', '/')
                val name = className.substring(className.lastIndexOf('.') + 1)
                AutoConfigurationClass(name, path)
            }.collect(Collectors.toCollection(Supplier { TreeSet() }))
        }

        companion object {
            fun of(metadataFile: File): AutoConfiguration {
                val metadata = Properties()
                try {
                    FileReader(metadataFile).use { reader ->
                        metadata.load(reader)
                    }
                } catch (ex: IOException) {
                    throw UncheckedIOException(ex)
                }
                return AutoConfiguration(
                    metadata.getProperty("module"), TreeSet<String?>(
                        StringUtils.commaDelimitedListToSet(metadata.getProperty("autoConfigurationClassNames"))
                    )
                )
            }
        }
    }

    class AutoConfigurationClass(val name: String, val path: String?) :
        Comparable<AutoConfigurationClass?> {
        override fun compareTo(other: AutoConfigurationClass): Int {
            return this.name.compareTo(other.name)
        }
    }
}
