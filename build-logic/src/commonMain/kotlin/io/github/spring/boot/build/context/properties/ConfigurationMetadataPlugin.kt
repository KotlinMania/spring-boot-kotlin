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

import org.gradle.api.*
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

/**
 * [Plugin] for projects that *only* define manual configuration metadata.
 * When applied, the plugin registers a [CheckManualSpringConfigurationMetadata]
 * task and configures the `check` task to depend upon it.
 * 
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class ConfigurationMetadataPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin>(
            JavaPlugin::class.java) { javaPlugin: JavaPlugin -> registerCheckAdditionalMetadataTask(project) }
    }

    private fun registerCheckAdditionalMetadataTask(project: Project) {
        val checkConfigurationMetadata: TaskProvider<CheckManualSpringConfigurationMetadata> = project.getTasks()
            .register<CheckManualSpringConfigurationMetadata>(
                CHECK_MANUAL_SPRING_CONFIGURATION_METADATA_TASK_NAME,
                CheckManualSpringConfigurationMetadata::class.java
            )
        val mainSourceSet: SourceSet = project.getExtensions()
            .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
            .sourceSets
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val manualMetadataLocation = project.getTasks()
            .named<ProcessResources>(mainSourceSet.getProcessResourcesTaskName(), ProcessResources::class.java)
            .map<File>(Transformer { processResources: ProcessResources? ->
                File(
                    processResources!!.getDestinationDir(),
                    "META-INF/spring-configuration-metadata.json"
                )
            })
        checkConfigurationMetadata.configure { check: CheckManualSpringConfigurationMetadata ->
            check!!.metadataLocation.set(manualMetadataLocation)
            check.reportLocation
                .set(
                    project.getLayout()
                        .getBuildDirectory()
                        .file("reports/manual-spring-configuration-metadata/check.txt")
                )
        }
        addMetadataArtifact(project, manualMetadataLocation)
        project.getTasks()
            .named(LifecycleBasePlugin.CHECK_TASK_NAME)
            .configure { check: Task -> check!!.dependsOn(checkConfigurationMetadata) }
    }

    private fun addMetadataArtifact(project: Project, metadataLocation: Provider<File>) {
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
        project.getArtifacts().add(CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME, metadataLocation)
    }

    companion object {
        private const val CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME = "configurationPropertiesMetadata"

        /**
         * Name of the [CheckAdditionalSpringConfigurationMetadata] task.
         */
        const val CHECK_MANUAL_SPRING_CONFIGURATION_METADATA_TASK_NAME: String =
            "checkManualSpringConfigurationMetadata"
    }
}
