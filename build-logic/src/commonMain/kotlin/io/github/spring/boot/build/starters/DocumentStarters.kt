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
package org.springframework.boot.build.starters

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.*
import org.springframework.util.StringUtils
import java.io.*
import java.util.Properties
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream
import org.gradle.api.file.DirectoryProperty

/**
 * [Task] to document all starter projects.
 * 
 * @author Andy Wilkinson
 */
abstract class DocumentStarters : DefaultTask() {
    private val starters: Configuration

    init {
        this.starters = getProject().getConfigurations().create("starters")
        getProject().getGradle().projectsEvaluated(Action { gradle: Gradle ->
            gradle!!.allprojects(Action { project: Project ->
                if (project!!.getPlugins().hasPlugin(StarterPlugin::class.java)) {
                    val dependency: MutableMap<String?, String?> = HashMap<String?, String?>()
                    dependency.put("path", project.getPath())
                    dependency.put("configuration", "starterMetadata")
                    this.starters.getDependencies().add(project.getDependencies().project(dependency))
                }
            })
        })
    }

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getStarters(): FileCollection {
        return this.starters
    }

    @TaskAction
    fun documentStarters() {
        val starters: MutableSet<Starter?> = this.starters.files
            .stream()
            .map<Starter> { metadata: File? -> this.loadStarter(metadata!!) }
            .collect(Collectors.toCollection(Supplier { TreeSet() }))
        writeTable("application-starters", starters.stream().filter { obj: Starter? -> obj!!.isApplication })
        writeTable("production-starters", starters.stream().filter { obj: Starter? -> obj!!.isProduction })
        writeTable("technical-starters", starters.stream().filter { obj: Starter? -> obj!!.isTechnical })
    }

    private fun loadStarter(metadata: File): Starter {
        val properties = Properties()
        try {
            FileReader(metadata).use { reader ->
                properties.load(reader)
                return Starter(
                    properties.getProperty("name"), properties.getProperty("description"),
                    StringUtils.commaDelimitedListToSet(properties.getProperty("dependencies"))
                )
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    private fun writeTable(name: String?, starters: Stream<Starter?>) {
        val output = File(this.outputDir.asFile.get(), name + ".adoc")
        output.parentFile.mkdirs()
        try {
            PrintWriter(FileWriter(output)).use { writer ->
                writer.println("|===")
                writer.println("| Name | Description")
                starters.forEach { starter: Starter? ->
                    writer.println()
                    writer.printf("| [[%s]]`%s`%n", starter.name, starter.name)
                    writer.printf("| %s%n", postProcessDescription(starter.description))
                }
                writer.println("|===")
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    private fun postProcessDescription(description: String): String {
        return addStarterCrossLinks(description)
    }

    private fun addStarterCrossLinks(input: String): String {
        return input.replace("(spring-boot-starter[A-Za-z0-9-]*)".toRegex(), "xref:#$1[`$1`]")
    }

    private class Starter(
        private val name: String,
        private val description: String,
        private val dependencies: MutableSet<String?>
    ) : Comparable<Starter?> {
        val isProduction: Boolean
            get() = this.name == "spring-boot-starter-actuator"

        val isTechnical: Boolean
            get() = !mutableListOf<String?>("spring-boot-starter", "spring-boot-starter-test")
                .contains(this.name) && !this.isProduction && !this.dependencies.contains("spring-boot-starter")

        val isApplication: Boolean
            get() = !this.isProduction && !this.isTechnical

        override fun compareTo(other: Starter): Int {
            return this.name.compareTo(other.name)
        }
    }
}
