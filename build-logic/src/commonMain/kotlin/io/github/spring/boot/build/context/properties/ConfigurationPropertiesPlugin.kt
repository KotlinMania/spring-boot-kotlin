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
package org.springframework.boot.build.context.properties

import org.gradle.kotlin.dsl.*

import org.gradle.api.*
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.springframework.util.StringUtils
import java.io.File
import java.util.Map
import java.util.stream.Collectors

/**
 * [Plugin] for projects that define `@ConfigurationProperties`. When applied,
 * the plugin reacts to the presence of the [JavaPlugin] by:
 * 
 * 
 *  * Adding a dependency on the configuration properties annotation processor.
 *  * Disables incremental compilation to avoid property descriptions being lost.
 *  * Configuring the additional metadata locations annotation processor compiler
 * argument.
 *  * Adding the outputs of the processResources task as inputs of the compileJava task
 * to ensure that the additional metadata is available when the annotation processor runs.
 *  * Registering a [CheckAdditionalSpringConfigurationMetadata] task and
 * configuring the `check` task to depend upon it.
 *  * Defining an artifact for the resulting configuration property metadata so that it
 * can be consumed by downstream projects.
 * 
 * 
 * @author Andy Wilkinson
 */
class ConfigurationPropertiesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin>().configureEach { val javaPlugin = this;
            configureConfigurationPropertiesAnnotationProcessor(project)
            disableIncrementalCompilation(project)
            configureAdditionalMetadataLocationsCompilerArgument(project)
            registerCheckAdditionalMetadataTask(project)
            registerCheckMetadataTask(project)
            addMetadataArtifact(project)
        }
    }

    private fun configureConfigurationPropertiesAnnotationProcessor(project: Project) {
        val annotationProcessors = project.getConfigurations()
            .getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
        annotationProcessors.getDependencies()
            .add(
                project.getDependencies()
                    .project(
                        Map.of<String?, String?>(
                            "path",
                            ":configuration-metadata:spring-boot-configuration-processor"
                        )
                    )
            )
    }

    private fun disableIncrementalCompilation(project: Project) {
        val mainSourceSet: SourceSet = project.getExtensions()
            .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
            .sourceSets
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        project.getTasks()
            .named<JavaCompile>(mainSourceSet.getCompileJavaTaskName(), JavaCompile::class.java)
            .configure { compileJava: JavaCompile -> compileJava!!.getOptions().setIncremental(false) }
    }

    private fun addMetadataArtifact(project: Project) {
        val mainSourceSet: SourceSet = project.getExtensions()
            .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
            .sourceSets
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        project.getConfigurations()
            .consumable(
                CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME) { configuration: ConsumableConfiguration ->
                    configuration!!.attributes { attributes: AttributeContainer ->
                            attributes!!.attribute<Category>(
                                Category.CATEGORY_ATTRIBUTE,
                                project.getObjects().named<Category>(Category::class.java, Category.DOCUMENTATION)
                            )
                            attributes.attribute<Usage>(
                                Usage.USAGE_ATTRIBUTE,
                                project.getObjects()
                                    .named<Usage>(Usage::class.java, "configuration-properties-metadata")
                            )
                        }
                }
        project.afterEvaluate { evaluatedProject: Project ->
            evaluatedProject!!.getArtifacts()
                .add(
                    CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME,
                    mainSourceSet.java
                        .destinationDirectory
                        .dir("META-INF/spring-configuration-metadata.json")) { artifact: ConfigurablePublishArtifact ->
                        artifact!!
                            .builtBy(evaluatedProject.getTasks().getByName(mainSourceSet.getClassesTaskName()))
                    }
        }
    }

    private fun configureAdditionalMetadataLocationsCompilerArgument(project: Project) {
        val compileJava = project.getTasks()
            .withType<JavaCompile>(JavaCompile::class.java)
            .getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME)
        compileJava.getInputs()
            .files(project.getTasks().getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME))
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .withPropertyName("processed resources")
        val mainSourceSet: SourceSet = project.getExtensions()
            .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
            .sourceSets
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        compileJava.getOptions()
            .getCompilerArgs()
            .add(
                "-Aorg.springframework.boot.configurationprocessor.additionalMetadataLocations="
                        + StringUtils.collectionToCommaDelimitedString(
                    mainSourceSet.getResources()
                        .getSourceDirectories()
                        .files
                        .stream()
                        .map<String> { path: File? -> project.getRootProject().relativePath(path!!) }
                        .collect(Collectors.toSet())))
    }

    private fun registerCheckAdditionalMetadataTask(project: Project) {
        val checkConfigurationMetadata: TaskProvider<CheckAdditionalSpringConfigurationMetadata> = project.getTasks()
            .register<CheckAdditionalSpringConfigurationMetadata>(
                CHECK_ADDITIONAL_SPRING_CONFIGURATION_METADATA_TASK_NAME,
                CheckAdditionalSpringConfigurationMetadata::class.java
            )
        checkConfigurationMetadata.configure { check: CheckAdditionalSpringConfigurationMetadata ->
            val mainSourceSet: SourceSet = project.getExtensions()
                .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
                .sourceSets
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            check!!.source = mainSourceSet.getResources()
            check.include("META-INF/additional-spring-configuration-metadata.json")
            check.reportLocation
                .set(
                    project.getLayout()
                        .getBuildDirectory()
                        .file("reports/additional-spring-configuration-metadata/check.txt")
                )
        }
        project.getTasks()
            .named(LifecycleBasePlugin.CHECK_TASK_NAME)
            .configure { check: Task -> check!!.dependsOn(checkConfigurationMetadata) }
    }

    private fun registerCheckMetadataTask(project: Project) {
        val checkConfigurationMetadata: TaskProvider<CheckSpringConfigurationMetadata> = project.getTasks()
            .register<CheckSpringConfigurationMetadata>(
                CHECK_SPRING_CONFIGURATION_METADATA_TASK_NAME,
                CheckSpringConfigurationMetadata::class.java
            )
        checkConfigurationMetadata.configure { check: CheckSpringConfigurationMetadata ->
            val mainSourceSet: SourceSet = project.getExtensions()
                .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
                .sourceSets
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            val metadataLocation = project.getTasks()
                .named<JavaCompile>(mainSourceSet.getCompileJavaTaskName(), JavaCompile::class.java)
                .flatMap<RegularFile>(Transformer { javaCompile: JavaCompile? ->
                    javaCompile!!.destinationDirectory
                        .file("META-INF/spring-configuration-metadata.json")
                })
            check!!.metadataLocation.set(metadataLocation)
            check.reportLocation
                .set(project.getLayout().getBuildDirectory().file("reports/spring-configuration-metadata/check.txt"))
        }
        project.getTasks()
            .named(LifecycleBasePlugin.CHECK_TASK_NAME)
            .configure { check: Task -> check!!.dependsOn(checkConfigurationMetadata) }
    }

    companion object {
        private const val CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME = "configurationPropertiesMetadata"

        /**
         * Name of the [CheckAdditionalSpringConfigurationMetadata] task.
         */
        const val CHECK_ADDITIONAL_SPRING_CONFIGURATION_METADATA_TASK_NAME: String =
            "checkAdditionalSpringConfigurationMetadata"

        /**
         * Name of the [CheckSpringConfigurationMetadata] task.
         */
        const val CHECK_SPRING_CONFIGURATION_METADATA_TASK_NAME: String = "checkSpringConfigurationMetadata"
    }
}
