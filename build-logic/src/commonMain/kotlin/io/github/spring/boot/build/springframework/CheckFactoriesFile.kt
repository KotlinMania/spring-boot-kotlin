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
package org.springframework.boot.build.springframework

import org.gradle.kotlin.dsl.*

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.springframework.util.StringUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.function.Consumer
import org.gradle.api.file.FileTree
import org.gradle.api.file.DirectoryProperty

/**
 * [Task] that checks files loaded by [SpringFactoriesLoader].
 * 
 * @author Andy Wilkinson
 */
abstract class CheckFactoriesFile protected constructor(private val path: String) : DefaultTask() {
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
            .matching { include(this@CheckFactoriesFile.path) }
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
        this.source.forEach(Consumer { factoriesFile: File? -> this.check(factoriesFile!!) })
    }

    private fun check(factoriesFile: File) {
        val properties = load(factoriesFile)
        val problems: MutableMap<String?, MutableList<String?>> = LinkedHashMap<String?, MutableList<String?>>()
        for (name in properties.stringPropertyNames()) {
            val value = properties.getProperty(name)
            val classNames = StringUtils.commaDelimitedListToStringArray(value).toList()
            collectProblems(problems, name, classNames)
            val sortedValues = classNames.sorted()
            if (sortedValues != classNames) {
                val problemsForClassName = problems.computeIfAbsent(name) { k: String? -> ArrayList<String?>() }
                problemsForClassName.add("Entries should be sorted alphabetically")
            }
        }
        val outputFile = this.outputDirectory.file("failure-report.txt").get().asFile
        writeReport(factoriesFile, problems, outputFile)
        if (!problems.isEmpty()) {
            throw VerificationException("%s check failed. See '%s' for details".format(this.path, outputFile))
        }
    }

    private fun collectProblems(
        problems: MutableMap<String?, MutableList<String?>>,
        key: String?,
        classNames: List<String>
    ) {
        for (className in classNames) {
            if (!find(className)) {
                addNoFoundProblem(className, problems.computeIfAbsent(key) { k: String? -> ArrayList<String?>() })
            }
        }
    }

    private fun addNoFoundProblem(className: String, problemsForClassName: MutableList<String?>) {
        val binaryName = binaryNameOf(className)
        val foundBinaryForm = find(binaryName)
        problemsForClassName.add(
            if (!foundBinaryForm)
                "'%s' was not found".format(className)
            else
                "'%s' should be listed using its binary name '%s'".format(className, binaryName)
        )
    }

    private fun find(className: String): Boolean {
        for (root in this.classpath.files) {
            val classFilePath = className.replace(".", "/") + ".class"
            if (File(root, classFilePath).isFile()) {
                return true
            }
        }
        return false
    }

    private fun binaryNameOf(className: String): String {
        val lastDotIndex = className.lastIndexOf('.')
        return className.substring(0, lastDotIndex) + "$" + className.substring(lastDotIndex + 1)
    }

    private fun load(aotFactories: File): Properties {
        val properties = Properties()
        try {
            FileInputStream(aotFactories).use { input ->
                properties.load(input)
                return properties
            }
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }

    private fun writeReport(
        factoriesFile: File?,
        problems: MutableMap<String?, MutableList<String?>>,
        outputFile: File
    ) {
        outputFile.parentFile.mkdirs()
        val report = StringBuilder()
        if (!problems.isEmpty()) {
            report.append("Found problems in '%s':%n".format(factoriesFile))
            problems.forEach { (key: String?, problemsForKey: MutableList<String?>?) ->
                report.append("  - %s:%n".format(key))
                problemsForKey!!.forEach(Consumer { problem: String? -> report.append("    - %s%n".format(problem)) })
            }
        }
        try {
            Files.writeString(
                outputFile.toPath(), report.toString(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }
}
