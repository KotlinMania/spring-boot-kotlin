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

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.jar.JarEntry
import java.util.jar.JarFile
import org.gradle.api.file.DirectoryProperty

/**
 * Task to check a project's `@AutoConfiguration` classes.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckAutoConfigurationClasses : AutoConfigurationImportsTask() {
    private var classpath: FileCollection = getProject().getObjects().fileCollection()

    private var optionalDependencies: FileCollection = getProject().getObjects().fileCollection()

    private var requiredDependencies: FileCollection = getProject().getObjects().fileCollection()

    private val optionalDependencyClassNames = getProject().getObjects().setProperty<String>(String::class.java)

    private val requiredDependencyClassNames = getProject().getObjects().setProperty<String>(String::class.java)

    init {
        this.outputDirectory.convention(getProject().getLayout().getBuildDirectory().dir(getName()))
        setGroup(LifecycleBasePlugin.VERIFICATION_GROUP)
        this.optionalDependencyClassNames.set(getProject().provider<MutableList<String?>>(Callable { classNamesOf(this.optionalDependencies) }))
        this.requiredDependencyClassNames.set(getProject().provider<MutableList<String?>>(Callable { classNamesOf(this.requiredDependencies) }))
    }

    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath
    }

    fun setClasspath(classpath: Any) {
        this.classpath = getProject().getObjects().fileCollection().from(classpath)
    }

    @Classpath
    fun getOptionalDependencies(): FileCollection {
        return this.optionalDependencies
    }

    fun setOptionalDependencies(classpath: Any) {
        this.optionalDependencies = getProject().getObjects().fileCollection().from(classpath)
    }

    @Classpath
    fun getRequiredDependencies(): FileCollection {
        return this.requiredDependencies
    }

    fun setRequiredDependencies(classpath: Any) {
        this.requiredDependencies = getProject().getObjects().fileCollection().from(classpath)
    }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val omittedFromImports: SetProperty<String>

    @TaskAction
    fun execute() {
        val problems: MutableMap<String?, MutableList<String?>?> = TreeMap<String?, MutableList<String?>?>()
        val optionalOnlyClassNames: MutableSet<String?> = HashSet<String?>(this.optionalDependencyClassNames.get())
        val requiredClassNames: MutableSet<String?> = this.requiredDependencyClassNames.get()
        optionalOnlyClassNames.removeAll(requiredClassNames)
        val imports = loadImports()
        classFiles().forEach(Consumer { classFile: File? ->
            val autoConfigurationClass: AutoConfigurationClass? = AutoConfigurationClass.Companion.of(classFile)
            if (autoConfigurationClass != null) {
                check(autoConfigurationClass, optionalOnlyClassNames, requiredClassNames, imports, problems)
            }
        })
        val outputFile = this.outputDirectory.file("failure-report.txt").get().asFile
        writeReport(problems, outputFile)
        if (!problems.isEmpty()) {
            throw VerificationException(
                "Auto-configuration class check failed. See '%s' for details".format(outputFile)
            )
        }
    }

    private fun classFiles(): MutableList<File?> {
        val classFiles: MutableList<File?> = ArrayList<File?>()
        for (root in this.classpath.files) {
            if (root.exists()) {
                try {
                    Files.walk(root.toPath()).use { files ->
                        files.forEach { file: Path? ->
                            if (Files.isRegularFile(file) && file!!.getFileName().toString().endsWith(".class")) {
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
        optionalOnlyClassNames: MutableSet<String?>,
        requiredClassNames: MutableSet<String?>,
        imports: MutableList<String?>,
        problems: MutableMap<String?, MutableList<String?>?>
    ) {
        if (!autoConfigurationClass.name.endsWith("AutoConfiguration")) {
            problems.computeIfAbsent(autoConfigurationClass.name) { name: kotlin.String? -> java.util.ArrayList<kotlin.String?>() }!!
                .add("Name of a class annotated with @AutoConfiguration should end with AutoConfiguration")
        }
        val testAutoConfiguration = autoConfigurationClass.name.endsWith("TestAutoConfiguration")
        if (!this.omittedFromImports.getOrElse(mutableSetOf<String?>())
                .contains(autoConfigurationClass.name) && !imports.contains(autoConfigurationClass.name) && !testAutoConfiguration
        ) {
            problems.computeIfAbsent(autoConfigurationClass.name) { name: kotlin.String? -> java.util.ArrayList<kotlin.String?>() }!!
                .add("Class is not registered in AutoConfiguration.imports")
        }
        if ((this.omittedFromImports.getOrElse(mutableSetOf<String?>()).contains(autoConfigurationClass.name)
                    || testAutoConfiguration) && imports.contains(autoConfigurationClass.name)
        ) {
            problems.computeIfAbsent(autoConfigurationClass.name) { name: kotlin.String? -> java.util.ArrayList<kotlin.String?>() }!!
                .add("Class should not be registered in AutoConfiguration.imports")
        }
        autoConfigurationClass.before.forEach(Consumer { before: String? ->
            if (optionalOnlyClassNames.contains(before)) {
                problems.computeIfAbsent(autoConfigurationClass.name) { name: kotlin.String? -> java.util.ArrayList<kotlin.String?>() }!!
                    .add(
                        "before '%s' is from an optional dependency and should be declared in beforeName"
                            .format(before)
                    )
            }
        })
        autoConfigurationClass.beforeName.forEach(Consumer { beforeName: String? ->
            if (!optionalOnlyClassNames.contains(beforeName)) {
                val problem: String = if (requiredClassNames.contains(beforeName))
                    "beforeName '%s' is from a required dependency and should be declared in before"
                        .format(beforeName)
                else
                    "beforeName '%s' not found".format(beforeName)
                problems.computeIfAbsent(autoConfigurationClass.name) { name: kotlin.String? -> java.util.ArrayList<kotlin.String?>() }!!
                    .add(problem)
            }
        })
        autoConfigurationClass.after.forEach(Consumer { after: String? ->
            if (optionalOnlyClassNames.contains(after)) {
                problems.computeIfAbsent(autoConfigurationClass.name) { name: kotlin.String? -> java.util.ArrayList<kotlin.String?>() }!!
                    .add(
                        "after '%s' is from an optional dependency and should be declared in afterName"
                            .format(after)
                    )
            }
        })
        autoConfigurationClass.afterName.forEach(Consumer { afterName: String? ->
            if (!optionalOnlyClassNames.contains(afterName)) {
                val problem: String = if (requiredClassNames.contains(afterName))
                    "afterName '%s' is from a required dependency and should be declared in after"
                        .format(afterName)
                else
                    "afterName '%s' not found".format(afterName)
                problems.computeIfAbsent(autoConfigurationClass.name) { name: kotlin.String? -> java.util.ArrayList<kotlin.String?>() }!!
                    .add(problem)
            }
        })
    }

    private fun writeReport(problems: MutableMap<String?, MutableList<String?>?>, outputFile: File) {
        outputFile.parentFile.mkdirs()
        val report = StringBuilder()
        if (!problems.isEmpty()) {
            report.append("Found auto-configuration class problems:%n".format())
            problems.forEach { (className: String?, classProblems: MutableList<String?>?) ->
                report.append("  - %s:%n".format(className))
                classProblems!!.forEach(Consumer { problem: String? -> report.append("    - %s%n".format(problem)) })
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
        private fun classNamesOf(classpath: FileCollection): MutableList<String?> {
            return classpath.files.stream().flatMap<String> { file: File? ->
                try {
                    JarFile(file).use { jarFile ->
                        return@flatMap Collections.list<JarEntry?>(jarFile.entries())
                            .stream()
                            .filter { entry: JarEntry? -> !entry!!.isDirectory() }
                            .map<String> { obj: JarEntry? -> obj!!.name }
                            .filter { entryName: String? -> entryName!!.endsWith(".class") }
                            .map<String> { entryName: String? ->
                                entryName!!.substring(
                                    0,
                                    entryName.length - ".class".length
                                )
                            }
                            .map<String> { entryName: String? -> entryName!!.replace("/", ".") }
                    }
                } catch (ex: IOException) {
                    throw UncheckedIOException(ex)
                }
            }.toList()
        }
    }
}
