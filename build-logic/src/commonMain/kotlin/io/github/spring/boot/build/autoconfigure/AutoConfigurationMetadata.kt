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
import org.springframework.asm.ClassReader
import org.springframework.asm.Opcodes
import org.springframework.core.CollectionFactory
import java.io.*
import java.lang.String
import java.util.*
import kotlin.Int
import kotlin.Throws
import kotlin.checkNotNull
import org.gradle.api.file.RegularFileProperty

/**
 * A [Task] for generating metadata describing a project's auto-configuration
 * classes.
 * 
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
abstract class AutoConfigurationMetadata : DefaultTask() {
    private val moduleName: String

    private var classesDirectories: FileCollection? = null

    init {
        this.moduleName = project.name
    }

    fun setSourceSet(sourceSet: SourceSet) {
        this.autoConfigurationImports.set(
            File(
                sourceSet.getOutput().getResourcesDir(),
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
            )
        )
        this.classesDirectories = sourceSet.getOutput().getClassesDirs()
        dependsOn(sourceSet.getOutput())
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val autoConfigurationImports: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @Classpath
    fun getClassesDirectories(): FileCollection {
        return this.classesDirectories!!
    }

    @TaskAction
    @Throws(IOException::class)
    fun documentAutoConfiguration() {
        val autoConfiguration = readAutoConfiguration()
        val outputFile = this.outputFile.get().asFile
        outputFile.parentFile.mkdirs()
        FileWriter(outputFile).use { writer ->
            autoConfiguration.store(writer, null)
        }
    }

    @Throws(IOException::class)
    private fun readAutoConfiguration(): Properties {
        val autoConfiguration = CollectionFactory.createSortedProperties(true)
        val classNames = readAutoConfigurationsFile()
        val publicClassNames: MutableSet<String?> = LinkedHashSet<String?>()
        for (className in classNames) {
            val classFile = findClassFile(className)
            checkNotNull(classFile) { "Auto-configuration class '" + className + "' not found." }
            FileInputStream(classFile).use { `in` ->
                val access = ClassReader(`in`).getAccess()
                if ((access and Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC) {
                    publicClassNames.add(className)
                }
            }
        }
        autoConfiguration.setProperty("autoConfigurationClassNames", String.join(",", publicClassNames))
        autoConfiguration.setProperty("module", this.moduleName)
        return autoConfiguration
    }

    /**
     * Reads auto-configurations from
     * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
     * @return auto-configurations
     */
    @Throws(IOException::class)
    private fun readAutoConfigurationsFile(): MutableList<kotlin.String> {
        val file = this.autoConfigurationImports.asFile.get()
        if (!file.exists()) {
            return mutableListOf<kotlin.String?>()
        }
        BufferedReader(FileReader(file)).use { reader ->
            return reader.lines().map<kotlin.String> { line: kotlin.String? -> this.stripComment(line!!) }
                .filter { line: kotlin.String? -> !line!!.isEmpty() }.toList()
        }
    }

    private fun stripComment(line: kotlin.String): kotlin.String {
        val commentStart: Int = line.indexOf(COMMENT_START)
        if (commentStart == -1) {
            return line.trim { it <= ' ' }
        }
        return line.substring(0, commentStart).trim { it <= ' ' }
    }

    private fun findClassFile(className: kotlin.String): File? {
        val classFileName = className.replace(".", "/") + ".class"
        for (classesDir in this.classesDirectories!!) {
            val classFile = File(classesDir, classFileName)
            if (classFile.isFile()) {
                return classFile
            }
        }
        return null
    }

    companion object {
        private const val COMMENT_START = "#"
    }
}
