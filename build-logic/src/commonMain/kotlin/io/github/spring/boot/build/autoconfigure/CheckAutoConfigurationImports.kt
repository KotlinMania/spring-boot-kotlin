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
package org.springframework.boot.build.autoconfigure

import org.gradle.kotlin.dsl.*

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationException
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import org.gradle.api.file.DirectoryProperty

/**
 * Task to check the contents of a project's
 * `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
 * file.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckAutoConfigurationImports : AutoConfigurationImportsTask() {
    private var classpath: FileCollection = project.getObjects().fileCollection()

    init {
        this.outputDirectory.convention(project.getLayout().getBuildDirectory().dir(name))
        setGroup(LifecycleBasePlugin.VERIFICATION_GROUP)
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
        val importsFile = source.singleFile
        check(importsFile)
    }

    private fun check(importsFile: File) {
        val imports = loadImports()
        val problems: MutableList<String?> = ArrayList<String?>()
        for (imported in imports) {
            val classFile = find(imported)
            if (classFile == null) {
                problems.add("'%s' was not found".format(imported))
            } else if (!correctlyAnnotated(classFile)) {
                problems.add("'%s' is not annotated with @AutoConfiguration".format(imported))
            }
        }
        val sortedValues: MutableList<String?> = ArrayList<String?>(imports)
        Collections.sort<String?>(sortedValues)
        if (sortedValues != imports) {
            val sortedOutputFile = this.outputDirectory.file("sorted-" + importsFile.name).get().asFile
            writeString(
                sortedOutputFile,
                sortedValues.stream().collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator()
            )
            problems.add(
                ("Entries should be sorted alphabetically (expect content written to "
                        + sortedOutputFile.absolutePath + ")")
            )
        }
        val outputFile = this.outputDirectory.file("failure-report.txt").get().asFile
        writeReport(importsFile, problems, outputFile)
        if (!problems.isEmpty()) {
            throw VerificationException(
                "%s check failed. See '%s' for details"
                    .format(AutoConfigurationImportsTask.Companion.IMPORTS_FILE, outputFile)
            )
        }
    }

    private fun find(className: String): File? {
        for (root in this.classpath.files) {
            val classFilePath = className.replace(".", "/") + ".class"
            val classFile = File(root, classFilePath)
            if (classFile.isFile()) {
                return classFile
            }
        }
        return null
    }

    private fun correctlyAnnotated(classFile: File?): Boolean {
        return AutoConfigurationClass.Companion.of(classFile) != null
    }

    private fun writeReport(importsFile: File?, problems: MutableList<String?>, outputFile: File) {
        outputFile.parentFile.mkdirs()
        val report = StringBuilder()
        if (!problems.isEmpty()) {
            report.append("Found problems in '%s':%n".format(importsFile))
            problems.forEach(Consumer { problem: String? -> report.append("  - %s%n".format(problem)) })
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
}
