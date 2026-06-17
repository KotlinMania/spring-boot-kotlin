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
package org.springframework.boot.build

import org.gradle.kotlin.dsl.*

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ConfigurationVariantDetails
import org.gradle.api.plugins.JavaTestFixturesPlugin

/**
 * Conventions that are applied in the presence of the [JavaTestFixturesPlugin].
 * When the plugin is applied:
 * 
 * 
 *  * Publishing of the test fixtures is disabled.
 * 
 * 
 * @author Andy Wilkinson
 */
class TestFixturesConventions {
    fun apply(project: Project) {
        project.plugins.withType<JavaTestFixturesPlugin>().configureEach { disablePublishing(project) }
    }

    private fun disablePublishing(project: Project) {
        val configurations = project.getConfigurations()
        val javaComponent = project.getComponents()
            .getByName("java") as AdhocComponentWithVariants
        javaComponent.withVariantsFromConfiguration(
            configurations.getByName("testFixturesApiElements")) { skip() }
        javaComponent.withVariantsFromConfiguration(
            configurations.getByName("testFixturesRuntimeElements")) { skip() }
    }
}
