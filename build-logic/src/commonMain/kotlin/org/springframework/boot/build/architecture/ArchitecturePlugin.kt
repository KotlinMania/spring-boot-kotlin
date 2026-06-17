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

import org.gradle.api.*
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.springframework.util.StringUtils

/**
 * [Plugin] for verifying a project's architecture.
 * 
 * @author Andy Wilkinson
 */
class ArchitecturePlugin : Plugin<Project?> {
    override fun apply(project: Project) {
        project.getPlugins()
            .withType<JavaPlugin?>(JavaPlugin::class.java, Action { javaPlugin: JavaPlugin? -> registerTasks(project) })
    }

    private fun registerTasks(project: Project) {
        val javaPluginExtension =
            project.getExtensions().getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
        for (sourceSet in javaPluginExtension.getSourceSets()) {
            registerArchitectureCheck(sourceSet, "java", project)
                .configure(Action { task: ArchitectureCheck? ->
                    task!!.setClasses(
                        project.files(
                            project.getTasks()
                                .named<JavaCompile?>(sourceSet.getCompileTaskName("java"), JavaCompile::class.java)
                                .flatMap<Directory?>(Transformer { compile: JavaCompile? -> compile!!.getDestinationDirectory() })
                        )
                    )
                })
            project.getPlugins()
                .withId(
                    "org.jetbrains.kotlin.jvm",
                    Action { kotlinPlugin: Plugin<*>? ->
                        registerArchitectureCheck(sourceSet, "kotlin", project)
                            .configure(Action { task: ArchitectureCheck? ->
                                task!!.setClasses(
                                    project.files(
                                        project.getTasks()
                                            .named<KotlinCompileTool?>(
                                                sourceSet.getCompileTaskName("kotlin"),
                                                KotlinCompileTool::class.java
                                            )
                                            .flatMap<Directory?>(Transformer { compile: KotlinCompileTool? -> compile!!.destinationDirectory })
                                    )
                                )
                            })
                    })
        }
    }

    private fun registerArchitectureCheck(
        sourceSet: SourceSet, language: String,
        project: Project
    ): TaskProvider<ArchitectureCheck?> {
        val checkArchitecture = project.getTasks()
            .register<ArchitectureCheck?>(
                "checkArchitecture"
                        + StringUtils.capitalize(sourceSet.getName() + StringUtils.capitalize(language)),
                ArchitectureCheck::class.java, Action { task: ArchitectureCheck? ->
                    task!!.getSourceSet().set(sourceSet.getName())
                    task.getCompileClasspath().from(sourceSet.getCompileClasspath())
                    task.getResourcesDirectory().set(sourceSet.getOutput().getResourcesDir())
                    task.dependsOn(sourceSet.getProcessResourcesTaskName())
                    task.setDescription(
                        ("Checks the architecture of the " + language + " classes of the "
                                + sourceSet.getName() + " source set.")
                    )
                    task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP)
                })
        project.getTasks()
            .named(LifecycleBasePlugin.CHECK_TASK_NAME)
            .configure(Action { check: Task? -> check!!.dependsOn(checkArchitecture) })
        return checkArchitecture
    }
}
