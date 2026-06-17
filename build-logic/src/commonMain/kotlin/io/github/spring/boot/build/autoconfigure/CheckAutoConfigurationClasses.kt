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

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.jar.JarFile
import org.gradle.api.file.DirectoryProperty

/**
 * Task to check a project's `@AutoConfiguration` classes.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckAutoConfigurationClasses : AutoConfigurationImportsTask() {
    private var classpath: FileCollection = project.getObjects().fileCollection()

    private var optionalDependencies: FileCollection = project.getObjects().fileCollection()

    private var requiredDependencies: FileCollection = project.getObjects().fileCollection()

    private val optionalDependencyClassNames = project.getObjects().setProperty<String>()

    private val requiredDependencyClassNames = project.getObjects().setProperty<String>()

    init {
        this.outputDirectory.convention(project.getLayout().getBuildDirectory().dir(name))
        setGroup(LifecycleBasePlugin.VERIFICATION_GROUP)
        this.optionalDependencyClassNames.set(project.provider { classNamesOf(this.optionalDependencies) })
        this.requiredDependencyClassNames.set(project.provider { classNamesOf(this.requiredDependencies) })
    }

    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath
    }

    fun setClasspath(classpath: Any) {
        this.classpath = project.getObjects().fileCollection().from(classpath)
    }

    @Classpath
    fun getOptionalDependencies(): FileCollection {
        return this.optionalDependencies
    }

    fun setOptionalDependencies(classpath: Any) {
        this.optionalDependencies = project.getObjects().fileCollection().from(classpath)
    }

    @Classpath
    fun getRequiredDependencies(): FileCollection {
        return this.requiredDependencies
    }

    fun setRequiredDependencies(classpath: Any) {
        this.requiredDependencies = project.getObjects().fileCollection().from(classpath)
    }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val omittedFromImports: SetProperty<String>

    @TaskAction
    fun execute() {
        val problems = sortedMapOf<String, MutableList<String>>()
        val optionalOnlyClassNames = this.optionalDependencyClassNames.get().toMutableSet()
        val requiredClassNames = this.requiredDependencyClassNames.get()
        optionalOnlyClassNames.removeAll(requiredClassNames)
        val imports = loadImports()
        for (classFile in classFiles()) {
            val autoConfigurationClass = AutoConfigurationClass.Companion.of(classFile)
            if (autoConfigurationClass != null) {
                check(autoConfigurationClass, optionalOnlyClassNames, requiredClassNames, imports, problems)
            }
        }
        val outputFile = this.outputDirectory.file("failure-report.txt").get().asFile
        writeReport(problems, outputFile)
        if (problems.isNotEmpty()) {
            throw VerificationException(
                "Auto-configuration class check failed. See '%s' for details".format(outputFile)
            )
        }
    }

    private fun classFiles(): List<File> {
        val classFiles = mutableListOf<File>()
        for (root in this.classpath.files) {
            if (root.exists()) {
                try {
                    Files.walk(root.toPath()).use { files ->
                        files.forEach { file ->
                            if (Files.isRegularFile(file) && file.fileName.toString().endsWith(".class")) {
                                classFiles.add(file.toFile())
                            }
                        }
                    }
                } catch (ex: IOException) {
                    throw UncheckedIOException(ex)
                }
            }
        }
        return classFiles
    }

    private fun check(
        autoConfigurationClass: AutoConfigurationClass,
        optionalOnlyClassNames: Set<String>,
        requiredClassNames: Set<String>,
        imports: List<String>,
        problems: MutableMap<String, MutableList<String>>
    ) {
        val name = autoConfigurationClass.name
        if (!name.endsWith("AutoConfiguration")) {
            problems.getOrPut(name) { mutableListOf() }
                .add("Name of a class annotated with @AutoConfiguration should end with AutoConfiguration")
        }
        val testAutoConfiguration = name.endsWith("TestAutoConfiguration")
        val omitted = this.omittedFromImports.getOrElse(emptySet())
        if (!omitted.contains(name) && !imports.contains(name) && !testAutoConfiguration) {
            problems.getOrPut(name) { mutableListOf() }
                .add("Class is not registered in AutoConfiguration.imports")
        }
        if ((omitted.contains(name) || testAutoConfiguration) && imports.contains(name)) {
            problems.getOrPut(name) { mutableListOf() }
                .add("Class should not be registered in AutoConfiguration.imports")
        }
        for (before in autoConfigurationClass.before) {
            if (optionalOnlyClassNames.contains(before)) {
                problems.getOrPut(name) { mutableListOf() }
                    .add(
                        "before '%s' is from an optional dependency and should be declared in beforeName"
                            .format(before)
                    )
            }
        }
        for (beforeName in autoConfigurationClass.beforeName) {
            if (!optionalOnlyClassNames.contains(beforeName)) {
                val problem = if (requiredClassNames.contains(beforeName))
                    "beforeName '%s' is from a required dependency and should be declared in before"
                        .format(beforeName)
                else
                    "beforeName '%s' not found".format(beforeName)
                problems.getOrPut(name) { mutableListOf() }.add(problem)
            }
        }
        for (after in autoConfigurationClass.after) {
            if (optionalOnlyClassNames.contains(after)) {
                problems.getOrPut(name) { mutableListOf() }
                    .add(
                        "after '%s' is from an optional dependency and should be declared in afterName"
                            .format(after)
                    )
            }
        }
        for (afterName in autoConfigurationClass.afterName) {
            if (!optionalOnlyClassNames.contains(afterName)) {
                val problem = if (requiredClassNames.contains(afterName))
                    "afterName '%s' is from a required dependency and should be declared in after"
                        .format(afterName)
                else
                    "afterName '%s' not found".format(afterName)
                problems.getOrPut(name) { mutableListOf() }.add(problem)
            }
        }
    }

    private fun writeReport(problems: Map<String, List<String>>, outputFile: File) {
        outputFile.parentFile.mkdirs()
        val report = StringBuilder()
        if (problems.isNotEmpty()) {
            report.append("Found auto-configuration class problems:%n".format())
            problems.forEach { (className, classProblems) ->
                report.append("  - %s:%n".format(className))
                classProblems.forEach { problem -> report.append("    - %s%n".format(problem)) }
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

    companion object {
        private fun classNamesOf(classpath: FileCollection): List<String> {
            return classpath.files.flatMap { file ->
                try {
                    JarFile(file).use { jarFile ->
                        jarFile.entries().asSequence()
                            .filter { !it.isDirectory }
                            .map { it.name }
                            .filter { it.endsWith(".class") }
                            .map { it.substring(0, it.length - ".class".length) }
                            .map { it.replace("/", ".") }
                            .toList()
                    }
                } catch (ex: IOException) {
                    throw UncheckedIOException(ex)
                }
            }
        }
    }
}
