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
package org.springframework.boot.build.optional

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/**
 * A `Plugin` that adds support for Maven-style optional dependencies. Creates a new
 * `optional` configuration. The `optional` configuration is part of the
 * project's compile and runtime classpaths but does not affect the classpath of dependent
 * projects.
 * 
 * @author Andy Wilkinson
 */
class OptionalDependenciesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val optional = project.getConfigurations().create("optional")
        optional.setCanBeConsumed(true)
        optional.setCanBeResolved(false)
        project.getPlugins().withType<JavaPlugin>(JavaPlugin::class.java) { javaPlugin: JavaPlugin ->
            val sourceSets: SourceSetContainer = project.getExtensions()
                .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
                .sourceSets
            sourceSets.all(Action { sourceSet: SourceSet ->
                project.getConfigurations()
                    .getByName(sourceSet!!.getCompileClasspathConfigurationName())
                    .extendsFrom(optional)
                project.getConfigurations()
                    .getByName(sourceSet.getRuntimeClasspathConfigurationName())
                    .extendsFrom(optional)
            })
        }
    }

    companion object {
        /**
         * Name of the `optional` configuration.
         */
        const val OPTIONAL_CONFIGURATION_NAME: String = "optional"
    }
}
