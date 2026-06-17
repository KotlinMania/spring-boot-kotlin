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
package org.springframework.boot.build.toolchain

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec

/**
 * [Plugin] for customizing Gradle's toolchain support.
 * 
 * @author Christoph Dreis
 * @author Andy Wilkinson
 */
class ToolchainPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        configureToolchain(project)
    }

    private fun configureToolchain(project: Project) {
        val toolchain =
            project.getExtensions().create<ToolchainExtension>("toolchain", ToolchainExtension::class.java, project)
        val toolchainVersion = toolchain.javaVersion
        if (toolchainVersion != null) {
            project.afterEvaluate { evaluated: Project -> configure(evaluated!!, toolchain) }
        }
    }

    private fun configure(project: Project, toolchain: ToolchainExtension) {
        if (!isJavaVersionSupported(toolchain, toolchain.javaVersion)) {
            disableToolchainTasks(project)
        } else {
            configureTestToolchain(project, toolchain.javaVersion)
        }
    }

    private fun isJavaVersionSupported(toolchain: ToolchainExtension, toolchainVersion: JavaLanguageVersion): Boolean {
        val minimumVersion = toolchain.minimumCompatibleJavaVersion.getOrNull()
        if (minimumVersion == null || toolchainVersion.canCompileOrRun(minimumVersion)) {
            return toolchain.maximumCompatibleJavaVersion
                .map<kotlin.Boolean>(org.gradle.api.Transformer { version: JavaLanguageVersion? ->
                    version.canCompileOrRun(
                        toolchainVersion
                    )
                })
                .getOrElse(true)!!
        }
        return false
    }

    private fun disableToolchainTasks(project: Project) {
        project.getTasks().withType<Test>(Test::class.java) { task: Test -> task!!.setEnabled(false) }
    }

    private fun configureTestToolchain(project: Project, toolchainVersion: JavaLanguageVersion?) {
        val javaToolchains = project.getExtensions().getByType<JavaToolchainService>(JavaToolchainService::class.java)
        project.getTasks()
            .withType<Test>(Test::class.java) { test: Test ->
                test!!.getJavaLauncher()
                    .set(javaToolchains.launcherFor { spec: JavaToolchainSpec ->
                        spec!!.getLanguageVersion().set(toolchainVersion)
                    })
            }
    }
}
