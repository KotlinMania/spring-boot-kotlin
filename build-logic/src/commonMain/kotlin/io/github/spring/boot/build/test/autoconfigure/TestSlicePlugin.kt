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
package org.springframework.boot.build.test.autoconfigure

import org.gradle.kotlin.dsl.*

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider

/**
 * [Plugin] for projects that define one or more test slices. When applied, it:
 * 
 * 
 *  * Applies the [TestAutoConfigurationPlugin]
 * 
 * Additionally, when the [JavaPlugin] is applied it:
 * 
 * 
 *  * Defines a task that produces metadata describing the test slices. The metadata is
 * made available as an artifact in the `testSliceMetadata` configuration
 * 
 * 
 * @author Andy Wilkinson
 */
class TestSlicePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val plugins = target.plugins
        plugins.apply<TestAutoConfigurationPlugin>(TestAutoConfigurationPlugin::class.java)
        plugins.withType<JavaPlugin>().configureEach { val plugin = this;
            val generateTestSliceMetadata = target.getTasks()
                .register<GenerateTestSliceMetadata>(
                    "generateTestSliceMetadata") { val task = this;
                        val mainSourceSet: SourceSet = target.getExtensions()
                            .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
                            .sourceSets
                            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                        task!!.setSourceSet(mainSourceSet)
                        task.outputFile
                            .set(target.getLayout().getBuildDirectory().file("test-slice-metadata.json"))
                    }
            addMetadataArtifact(target, generateTestSliceMetadata)
        }
    }

    private fun addMetadataArtifact(project: Project, task: TaskProvider<GenerateTestSliceMetadata>) {
        project.getConfigurations()
            .consumable(TEST_SLICE_METADATA_CONFIGURATION_NAME) {
                attributes {
                    attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.getObjects().named<Category>(Category.DOCUMENTATION)
                    )
                    attribute(
                        Usage.USAGE_ATTRIBUTE,
                        project.getObjects().named<Usage>("test-slice-metadata")
                    )
                }
            }
        project.getArtifacts().add(TEST_SLICE_METADATA_CONFIGURATION_NAME, task)
    }

    companion object {
        private const val TEST_SLICE_METADATA_CONFIGURATION_NAME = "testSliceMetadata"
    }
}
