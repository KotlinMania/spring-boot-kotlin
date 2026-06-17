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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.springframework.boot.build.DeployedPlugin
import org.springframework.boot.build.optional.OptionalDependenciesPlugin
import java.util.*
import java.util.stream.Collectors

/**
 * [Plugin] for projects that define auto-configuration. When applied, the plugin
 * applies the [DeployedPlugin]. Additionally, when the [JavaPlugin] is
 * applied it:
 * 
 * 
 *  * Adds a dependency on the auto-configuration annotation processor.
 *  * Defines a task that produces metadata describing the auto-configuration. The
 * metadata is made available as an artifact in the `autoConfigurationMetadata`
 * configuration.
 *  * Add checks to ensure import files and annotations are correct
 * 
 * 
 * @author Andy Wilkinson
 */
class AutoConfigurationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.getPlugins().apply<DeployedPlugin>(DeployedPlugin::class.java)
        project.getPlugins().withType<JavaPlugin>(
            JavaPlugin::class.java) { javaPlugin: JavaPlugin -> Configurer(project).configure() }
    }

    private class Configurer(private val project: Project) {
        private val main: SourceSet

        init {
            this.main = project.getExtensions()
                .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
                .sourceSets
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        }

        fun configure() {
            addAnnotationProcessorsDependencies()
            val tasks = this.project.getTasks()
            val configurations = this.project.getConfigurations()
            configurations.consumable(
                AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME) { configuration: ConsumableConfiguration ->
                    configuration!!.attributes(
                        Action { attributes: AttributeContainer ->
                            attributes!!.attribute<Category>(
                                Category.CATEGORY_ATTRIBUTE,
                                this.project.getObjects().named<Category>(Category::class.java, Category.DOCUMENTATION)
                            )
                            attributes.attribute<Usage>(
                                Usage.USAGE_ATTRIBUTE,
                                this.project.getObjects()
                                    .named<Usage>(Usage::class.java, "auto-configuration-metadata")
                            )
                        })
                }
            tasks.register<AutoConfigurationMetadata>(
                "autoConfigurationMetadata", AutoConfigurationMetadata::class.java) { task: AutoConfigurationMetadata -> this.configureAutoConfigurationMetadata(task) }
            val checkAutoConfigurationImports = tasks.register<CheckAutoConfigurationImports>(
                "checkAutoConfigurationImports", CheckAutoConfigurationImports::class.java) { task: CheckAutoConfigurationImports -> this.configureCheckAutoConfigurationImports(task) }
            val requiredClasspath = configurations.create("autoConfigurationRequiredClasspath")
                .extendsFrom(
                    configurations.getByName(this.main.getImplementationConfigurationName()),
                    configurations.getByName(this.main.getRuntimeOnlyConfigurationName())
                )
            requiredClasspath.getDependencies().add(projectDependency(":core:spring-boot-autoconfigure"))
            val checkAutoConfigurationClasses = tasks.register<CheckAutoConfigurationClasses>(
                "checkAutoConfigurationClasses", CheckAutoConfigurationClasses::class.java) { task: CheckAutoConfigurationClasses ->
                    configureCheckAutoConfigurationClasses(
                        requiredClasspath,
                        task!!
                    )
                }
            this.project.getPlugins()
                .withType<OptionalDependenciesPlugin>(
                    OptionalDependenciesPlugin::class.java) { plugin: OptionalDependenciesPlugin ->
                        configureCheckAutoConfigurationClassesForOptionalDependencies(
                            configurations,
                            checkAutoConfigurationClasses
                        )
                    }
            this.project.getTasks()
                .getByName(JavaBasePlugin.CHECK_TASK_NAME)
                .dependsOn(checkAutoConfigurationImports, checkAutoConfigurationClasses)
        }

        fun addAnnotationProcessorsDependencies() {
            this.project.getConfigurations()
                .getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                .getDependencies()
                .addAll(
                    projectDependencies(
                        ":core:spring-boot-autoconfigure-processor",
                        ":configuration-metadata:spring-boot-configuration-processor"
                    )
                )
        }

        fun configureAutoConfigurationMetadata(task: AutoConfigurationMetadata) {
            task.setSourceSet(this.main)
            task.dependsOn(this.main.getClassesTaskName())
            task.outputFile
                .set(this.project.getLayout().getBuildDirectory().file("auto-configuration-metadata.properties"))
            this.project.getArtifacts()
                .add(
                    AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME, task.outputFile) { artifact: ConfigurablePublishArtifact -> artifact!!.builtBy(task) }
        }

        fun configureCheckAutoConfigurationImports(task: CheckAutoConfigurationImports) {
            task.setSource(this.main.getResources())
            task.setClasspath(this.main.getOutput().getClassesDirs())
            task.setDescription(
                "Checks the %s file of the main source set.".format(AutoConfigurationImportsTask.Companion.IMPORTS_FILE)
            )
        }

        fun configureCheckAutoConfigurationClasses(
            requiredClasspath: Configuration?,
            task: CheckAutoConfigurationClasses
        ) {
            task.setSource(this.main.getResources())
            task.setClasspath(this.main.getOutput().getClassesDirs())
            task.setRequiredDependencies(requiredClasspath)
            task.setDescription("Checks the auto-configuration classes of the main source set.")
        }

        fun configureCheckAutoConfigurationClassesForOptionalDependencies(
            configurations: ConfigurationContainer,
            checkAutoConfigurationClasses: TaskProvider<CheckAutoConfigurationClasses>
        ) {
            checkAutoConfigurationClasses.configure(Action { check: CheckAutoConfigurationClasses ->
                val optionalClasspath = configurations.create("autoConfigurationOptionalClassPath")
                    .extendsFrom(configurations.getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME))
                check!!.setOptionalDependencies(optionalClasspath)
            })
        }

        fun projectDependencies(vararg paths: String?): MutableSet<Dependency?> {
            return Arrays.stream<String?>(paths).map<Dependency> { path: String? -> projectDependency(path) }.collect(
                Collectors.toSet()
            )
        }

        fun projectDependency(path: String?): Dependency {
            return this.project.getDependencies().project(Collections.singletonMap<String?, String?>("path", path))
        }
    }

    companion object {
        private const val AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME = "autoConfigurationMetadata"
    }
}
