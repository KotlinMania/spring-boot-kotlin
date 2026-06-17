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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin

/**
 * Plugin to apply conventions to projects that are part of Spring Boot's build.
 * Conventions are applied in response to various plugins being applied.
 * 
 * When the [JavaBasePlugin] is applied, the conventions in [JavaConventions]
 * are applied.
 * 
 * When the [MavenPublishPlugin] is applied, the conventions in
 * [MavenPublishingConventions] are applied.
 * 
 * When the [AntoraPlugin] is applied, the conventions in [AntoraConventions]
 * are applied.
 * 
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author Mike Smithson
 */
class ConventionsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val systemRequirements = project.getExtensions()
            .create<SystemRequirementsExtension>("systemRequirements", SystemRequirementsExtension::class.java)
        NoHttpConventions().apply(project)
        JavaConventions(systemRequirements).apply(project)
        MavenPublishingConventions().apply(project)
        KotlinConventions().apply(project)
        WarConventions().apply(project)
        EclipseConventions(systemRequirements).apply(project)
        TestFixturesConventions().apply(project)
        ProtobufConventions().apply(project)
        RepositoryTransformersExtension.Companion.apply(project)
    }
}
