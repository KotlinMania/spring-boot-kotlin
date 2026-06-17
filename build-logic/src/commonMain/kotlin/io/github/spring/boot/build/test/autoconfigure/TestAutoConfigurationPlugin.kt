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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * [Plugin] for projects that define test auto-configuration. When the
 * [JavaPlugin] is applied it:
 * 
 * 
 *  * Add checks to ensure AutoConfigure*.import files and related annotations are
 * correct
 * 
 * 
 * @author Andy Wilkinson
 */
class TestAutoConfigurationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.withType<JavaPlugin>(JavaPlugin::class.java) { plugin: JavaPlugin ->
            val checkAutoConfigureImports = target.getTasks()
                .register<CheckAutoConfigureImports>(
                    "checkAutoConfigureImports",
                    CheckAutoConfigureImports::class.java) { task: CheckAutoConfigureImports ->
                        val mainSourceSet: SourceSet = target.getExtensions()
                            .getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
                            .sourceSets
                            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                        task!!.source = mainSourceSet.getResources()
                        val classpath = target.files(
                            mainSourceSet.getRuntimeClasspath(),
                            target.getConfigurations().getByName(mainSourceSet.getRuntimeClasspathConfigurationName())
                        )
                        task.setClasspath(classpath)
                    }
            target.getTasks()
                .named(LifecycleBasePlugin.CHECK_TASK_NAME)
                .configure { check: Task -> check!!.dependsOn(checkAutoConfigureImports) }
        }
    }
}
