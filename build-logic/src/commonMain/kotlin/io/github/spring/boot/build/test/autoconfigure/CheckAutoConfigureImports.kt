/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.boot.build.test.autoconfigure

import org.gradle.kotlin.dsl.*

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.springframework.boot.build.autoconfigure.AutoConfigurationClass.Companion.of
import java.io.*
import java.lang.Boolean
import java.nio.file.Files
import java.util.*
import java.util.function.Consumer
import java.util.jar.JarFile
import java.util.stream.Collectors
import kotlin.Any
import kotlin.Comparator
import kotlin.String
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.toList
import kotlin.map
import kotlin.sequences.map
import kotlin.sequences.toList
import kotlin.text.StringBuilder
import kotlin.text.get
import kotlin.text.map
import kotlin.text.replace
import kotlin.text.startsWith
import kotlin.text.substring
import kotlin.text.toList
import kotlin.toList
import org.gradle.api.file.FileTree
import org.gradle.api.file.DirectoryProperty

/**
 * Task to check the contents of a project's
 * `META-INF/spring/ *.AutoConfigure*.imports` files.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckAutoConfigureImports : DefaultTask() {
    private var sourceFiles: FileCollection = project.getObjects().fileCollection()

    private var classpath: FileCollection = project.getObjects().fileCollection()

    init {
        this.outputDirectory.convention(project.getLayout().getBuildDirectory().dir(name))
        setGroup(LifecycleBasePlugin.VERIFICATION_GROUP)
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    var source: FileTree
        get() = this.sourceFiles.getAsFileTree()
            .matching { include("META-INF/spring/*.AutoConfigure*.imports") }
        set(source) {
            this.sourceFiles = project.getObjects().fileCollection().from(source)
        }

    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath
    }

    fun setClasspath(classpath: Any) {
        this.classpath = project.getObjects().fileCollection().from(classpath)
    }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun execute() {
        val allProblems: MutableMap<String?, MutableList<String?>?> = TreeMap<String?, MutableList<String?>?>()
        for (autoConfigureImports in loadImports()) {
            val problems: MutableList<String?> = ArrayList<String?>()
            if (!find(autoConfigureImports.annotationName!!)) {
                problems.add("Annotation '%s' was not found".format(autoConfigureImports.annotationName))
            }
            for (imported in autoConfigureImports.imports!!) {
                var importedClassName: String = imported!!
                if (importedClassName.startsWith("optional:")) {
                    importedClassName = importedClassName.substring("optional:".length)
                }
                val found = find(importedClassName, Consumer { input: InputStream? ->
                    if (!correctlyAnnotated(input!!)) {
                        problems.add(
                            "Imported auto-configuration '%s' is not annotated with @AutoConfiguration"
                                .format(imported)
                        )
                    }
                })
                if (!found) {
                    problems.add("Imported auto-configuration '%s' was not found".format(importedClassName))
                }
            }
            val sortedValues: MutableList<String?> = ArrayList<String?>(autoConfigureImports.imports)
            sortedValues.sortWith(compareBy({ it!!.startsWith("optional:") }, { it!! }))
            if (sortedValues != autoConfigureImports.imports) {
                val sortedOutputFile = this.outputDirectory.file("sorted-" + autoConfigureImports.fileName)
                    .get()
                    .asFile
                writeString(
                    sortedOutputFile, sortedValues.stream().collect(Collectors.joining(System.lineSeparator()))
                            + System.lineSeparator()
                )
                problems.add(
                    "Entries should be required then optional, each sorted alphabetically (expected content written to '%s')"
                        .format(sortedOutputFile.absolutePath)
                )
            }
            if (!problems.isEmpty()) {
                allProblems.computeIfAbsent(autoConfigureImports.fileName) { unused: kotlin.String? -> java.util.ArrayList<kotlin.String?>() }!!
                    .addAll(problems)
            }
        }
        val outputFile = this.outputDirectory.file("failure-report.txt").get().asFile
        writeReport(allProblems, outputFile)
        if (!allProblems.isEmpty()) {
            throw VerificationException(
                "AutoConfigure….imports checks failed. See '%s' for details".format(outputFile)
            )
        }
    }

    private fun loadImports(): MutableList<AutoConfigureImports> {
        return this.source.files.stream().map<AutoConfigureImports> { file: File? ->
            val fileName = file!!.name
            val annotationName = fileName.substring(0, fileName.length - ".imports".length)
            AutoConfigureImports(annotationName, loadImports(file), fileName)
        }.toList()
    }

    private fun loadImports(importsFile: File): MutableList<String?> {
        try {
            return Files.readAllLines(importsFile.toPath())
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }

    private fun find(
        className: String,
        handler: Consumer<InputStream?> = Consumer { input: InputStream? -> }
    ): kotlin.Boolean {
        for (root in this.classpath.files) {
            val classFilePath = className.replace(".", "/") + ".class"
            if (root.isDirectory()) {
                val classFile = File(root, classFilePath)
                if (classFile.isFile()) {
                    try {
                        FileInputStream(classFile).use { input ->
                            handler.accept(input)
                        }
                    } catch (ex: IOException) {
                        throw UncheckedIOException(ex)
                    }
                    return true
                }
            } else {
                try {
                    JarFile(root).use { jar ->
                        val entry = jar.getEntry(classFilePath)
                        if (entry != null) {
                            jar.getInputStream(entry).use { input ->
                                handler.accept(input)
                            }
                            return true
                        }
                    }
                } catch (ex: IOException) {
                    throw UncheckedIOException(ex)
                }
            }
        }
        return false
    }

    private fun correctlyAnnotated(classFile: InputStream): kotlin.Boolean {
        return of(classFile) != null
    }

    private fun writeReport(allProblems: MutableMap<String?, MutableList<String?>?>, outputFile: File) {
        outputFile.parentFile.mkdirs()
        val report = StringBuilder()
        if (!allProblems.isEmpty()) {
            allProblems.forEach { (fileName: String?, problems: MutableList<String?>?) ->
                report.append("Found problems in '%s':%n".format(fileName))
                problems!!.forEach(Consumer { problem: String? -> report.append("  - %s%n".format(problem)) })
            }
        }
        writeString(outputFile, report.toString())
    }

    private fun writeString(file: File, content: String) {
        try {
            Files.writeString(file.toPath(), content)
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }

    @JvmRecord
    data class AutoConfigureImports(
        val annotationName: String?,
        val imports: MutableList<String?>?,
        val fileName: String?
    )
}
