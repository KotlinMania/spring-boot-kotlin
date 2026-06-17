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

import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.springframework.boot.build.SystemRequirementsExtension
import java.util.concurrent.Callable
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty

/**
 * DSL extension for [ToolchainPlugin].
 * 
 * @author Christoph Dreis
 */
abstract class ToolchainExtension(project: Project) {
    val javaVersion: JavaLanguageVersion?

    init {
        val toolchainVersion = project.findProperty("toolchainVersion") as String?
        this.javaVersion = if (toolchainVersion != null) JavaLanguageVersion.of(toolchainVersion) else null
        val systemRequirements = project.getExtensions()
            .getByType<SystemRequirementsExtension>(SystemRequirementsExtension::class.java)
        this.minimumCompatibleJavaVersion
            .convention(project.provider<JavaLanguageVersion>(Callable {
                JavaLanguageVersion.of(
                    systemRequirements.java.version
                )
            }))
    }

    abstract val minimumCompatibleJavaVersion: Property<JavaLanguageVersion>

    abstract val maximumCompatibleJavaVersion: Property<JavaLanguageVersion>

    abstract val testJvmArgs: ListProperty<String>
}
