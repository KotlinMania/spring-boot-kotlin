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
package org.springframework.boot.build.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.EvaluationResult
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.Callable
import java.util.function.Supplier
import java.util.stream.Stream
import org.gradle.api.provider.Property
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ConfigurableFileCollection

/**
 * [Task] that checks for architecture problems.
 * 
 * @author Andy Wilkinson
 * @author Yanming Zhou
 * @author Scott Frederick
 * @author Ivan Malutin
 * @author Phillip Webb
 * @author Dmytro Nosan
 * @author Moritz Halbritter
 * @author Stefano Cordio
 */
abstract class ArchitectureCheck : DefaultTask() {
    private var classes: FileCollection = null

    init {
        this.outputDirectory.convention(project.getLayout().getBuildDirectory().dir(name))
        this.annotationClasses.convention(ArchitectureCheckAnnotation.Companion.asMap())
        this.rules.addAll(
            this.prohibitObjectsRequireNonNull.convention(true)
                .map<MutableList<ArchRule?>?>(whenTrue(Supplier { obj: ArchitectureRules? -> ArchitectureRules.noClassesShouldCallObjectsRequireNonNull() }))
        )
        this.rules.addAll(ArchitectureRules.standard())
        this.rules.addAll(whenMainSources(Supplier {
            ArchitectureRules.beanMethods(
                ArchitectureCheckAnnotation.Companion.classFor(
                    this.annotationClasses.get(), ArchitectureCheckAnnotation.CONDITIONAL_ON_CLASS
                )
            )
        }))
        this.rules.addAll(whenMainSources(Supplier {
            ArchitectureRules.conditionalOnMissingBean(
                ArchitectureCheckAnnotation.Companion.classFor(
                    this.annotationClasses.get(), ArchitectureCheckAnnotation.CONDITIONAL_ON_MISSING_BEAN
                )
            )
        }))
        this.rules.addAll(whenMainSources(Supplier {
            ArchitectureRules.configurationProperties(
                ArchitectureCheckAnnotation.Companion.classFor(
                    this.annotationClasses.get(), ArchitectureCheckAnnotation.CONFIGURATION_PROPERTIES
                )
            )
        }))
        this.rules.addAll(
            whenMainSources(
                Supplier {
                    ArchitectureRules.configurationPropertiesBinding(
                        ArchitectureCheckAnnotation.Companion.classFor(
                            this.annotationClasses.get(), ArchitectureCheckAnnotation.CONFIGURATION_PROPERTIES_BINDING
                        )
                    )
                })
        )
        this.rules.addAll(
            whenMainSources(
                Supplier {
                    ArchitectureRules.configurationPropertiesDeprecation(
                        ArchitectureCheckAnnotation.Companion.classFor(
                            this.annotationClasses.get(), ArchitectureCheckAnnotation.DEPRECATED_CONFIGURATION_PROPERTY
                        )
                    )
                })
        )
        this.rules.addAll(whenMainSources(Supplier {
            mutableListOf<ArchRule?>(
                ArchitectureRules.allCustomAssertionMethodsNotReturningSelfShouldBeAnnotatedWithCheckReturnValue()
            )
        }))
        this.ruleDescriptions.set(this.rules.map<MutableList<String?>?>(Transformer { rules: MutableList<ArchRule?>? ->
            this.asDescriptions(
                rules!!
            )
        }))
    }

    private fun whenMainSources(rules: Supplier<MutableList<ArchRule?>?>): Provider<MutableList<ArchRule?>> {
        return this.isMainSourceSet.map<MutableList<ArchRule?>?>(whenTrue(rules))
    }

    private val isMainSourceSet: Provider<Boolean>
        get() = this.sourceSet.convention(SourceSet.MAIN_SOURCE_SET_NAME)
            .map<Boolean>(Transformer { anObject: String? ->
                SourceSet.MAIN_SOURCE_SET_NAME.equals(anObject)
            })

    private fun whenTrue(rules: Supplier<MutableList<ArchRule?>?>): Transformer<MutableList<ArchRule?>?, Boolean?> {
        return Transformer { `in`: Boolean? -> if (!`in`!!) mutableListOf<ArchRule?>() else rules.get() }
    }

    private fun asDescriptions(rules: MutableList<ArchRule?>): MutableList<String?> {
        return rules.stream().map<String> { obj: ArchRule? -> obj!!.description }.toList()
    }

    @TaskAction
    @Throws(Exception::class)
    fun checkArchitecture() {
        withCompileClasspath(Callable {
            val javaClasses = ClassFileImporter().importPaths(classFilesPaths())
            val results: MutableList<EvaluationResult?> = ArrayList<EvaluationResult?>()
            evaluate(javaClasses)!!.forEach { e: EvaluationResult? -> results.add(e) }
            results.add(AutoConfigurationChecker().check(javaClasses))
            val outputFile = this.outputDirectory.file("failure-report.txt").get().asFile
            val violations: MutableList<EvaluationResult> =
                results.stream().filter { obj: EvaluationResult -> obj.hasViolation() }.toList()
            writeViolationReport(violations, outputFile)
            if (!violations.isEmpty()) {
                throw VerificationException("Architecture check failed. See '" + outputFile + "' for details.")
            }
            null
        })
    }

    private fun classFilesPaths(): MutableList<Path?> {
        return this.classes!!.files.stream().map<Path> { obj: File? -> obj!!.toPath() }.toList()
    }

    private fun evaluate(javaClasses: JavaClasses?): Stream<EvaluationResult?>? {
        return this.rules.get().stream().map<EvaluationResult> { rule: ArchRule? -> rule!!.evaluate(javaClasses) }
    }

    @Throws(Exception::class)
    private fun withCompileClasspath(callable: Callable<*>) {
        val previous = Thread.currentThread().getContextClassLoader()
        val urls: MutableList<URL?> = ArrayList<URL?>()
        for (file in this.compileClasspath.files) {
            urls.add(file.toURI().toURL())
        }
        URLClassLoader(urls.toTypedArray<URL?>(), javaClass.getClassLoader()).use { classLoader ->
            Thread.currentThread().setContextClassLoader(classLoader)
            try {
                callable.call()
            } finally {
                Thread.currentThread().setContextClassLoader(previous)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeViolationReport(violations: MutableList<EvaluationResult>, outputFile: File) {
        outputFile.parentFile.mkdirs()
        val report = StringBuilder()
        for (violation in violations) {
            report.append(violation.getFailureReport())
            report.append(String.format("%n"))
        }
        Files.writeString(
            outputFile.toPath(), report.toString(), StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    fun setClasses(classes: FileCollection) {
        this.classes = classes
    }

    @Internal
    fun getClasses(): FileCollection {
        return this.classes!!
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    @get:InputFiles
    val inputClasses: FileTree
        get() = this.classes!!.getAsFileTree()

    @get:Classpath
    @get:InputFiles
    abstract val compileClasspath: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    @get:Optional
    abstract val resourcesDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val rules: ListProperty<ArchRule>

    @get:Internal
    abstract val prohibitObjectsRequireNonNull: Property<Boolean>

    @get:Internal
    abstract val sourceSet: Property<String>

    @get:Input
    abstract val ruleDescriptions: ListProperty<String>

    @get:Internal
    abstract val annotationClasses: MapProperty<String, String>
}
