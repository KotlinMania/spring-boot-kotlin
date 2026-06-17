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
package org.springframework.boot.build.devtools

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import java.util.jar.JarFile
import org.gradle.api.file.RegularFileProperty

/**
 * Task for documenting Devtools' property defaults.
 * 
 * @author Andy Wilkinson
 */
abstract class DocumentDevtoolsPropertyDefaults : DefaultTask() {
    private var defaults: FileCollection = null

    init {
        this.outputFile.convention(
            getProject().getLayout()
                .getBuildDirectory()
                .file("generated/docs/using/devtools-property-defaults.adoc")
        )
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getDefaults(): FileCollection {
        return this.defaults!!
    }

    fun setDefaults(defaults: FileCollection) {
        this.defaults = defaults
    }

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    @Throws(IOException::class)
    fun documentPropertyDefaults() {
        val propertyDefaults = loadPropertyDefaults()
        documentPropertyDefaults(propertyDefaults)
    }

    @Throws(IOException::class, FileNotFoundException::class)
    private fun loadPropertyDefaults(): MutableMap<String?, String?> {
        val properties = Properties()
        val propertyDefaults: MutableMap<String?, String?> = TreeMap<String?, String?>()
        for (contribution in this.defaults!!.files) {
            if (contribution.isFile()) {
                JarFile(contribution).use { jar ->
                    val entry = jar.getEntry("META-INF/spring-devtools.properties")
                    if (entry != null) {
                        properties.load(jar.getInputStream(entry))
                    }
                }
            } else check(!contribution.exists()) { "Unexpected Devtools default properties contribution from '" + contribution + "'" }
        }
        for (name in properties.stringPropertyNames()) {
            if (name.startsWith("defaults.")) {
                propertyDefaults.put(name.substring("defaults.".length), properties.getProperty(name))
            }
        }
        return propertyDefaults
    }

    @Throws(IOException::class)
    private fun documentPropertyDefaults(properties: MutableMap<String?, String?>) {
        PrintWriter(FileWriter(this.outputFile.asFile.get())).use { writer ->
            writer.println("[cols=\"3,1\"]")
            writer.println("|===")
            writer.println("| Name | Default Value")
            properties.forEach { (name: String?, value: String?) ->
                writer.println()
                writer.printf("| `%s`%n", name)
                writer.printf("| `%s`%n", value)
            }
            writer.println("|===")
        }
    }
}
