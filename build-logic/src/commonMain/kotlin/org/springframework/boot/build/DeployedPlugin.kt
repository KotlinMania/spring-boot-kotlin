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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.bundling.Jar

/**
 * A plugin applied to a project that should be deployed.
 * 
 * @author Andy Wilkinson
 */
class DeployedPlugin : Plugin<Project?> {
    @Suppress("deprecation")
    override fun apply(project: Project) {
        project.getPlugins().apply<MavenPublishPlugin?>(MavenPublishPlugin::class.java)
        project.getPlugins().apply<MavenRepositoryPlugin?>(MavenRepositoryPlugin::class.java)
        val publishing = project.getExtensions().getByType<PublishingExtension>(PublishingExtension::class.java)
        val mavenPublication =
            publishing.getPublications().create<MavenPublication>("maven", MavenPublication::class.java)
        project.afterEvaluate(Action { evaluated: Project? ->
            project.getPlugins().withType<JavaPlugin?>(JavaPlugin::class.java).all(
                Action { javaPlugin: JavaPlugin? ->
                    if ((project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME) as Jar).isEnabled()) {
                        project.getComponents()
                            .matching(Spec { component: SoftwareComponent? -> component!!.getName() == "java" })
                            .all(Action { component: SoftwareComponent? -> mavenPublication.from(component) })
                    }
                })
        })
        project.getPlugins()
            .withType<JavaPlatformPlugin?>(JavaPlatformPlugin::class.java)
            .all(Action { javaPlugin: JavaPlatformPlugin? ->
                project.getComponents()
                    .matching(Spec { component: SoftwareComponent? -> component!!.getName() == "javaPlatform" })
                    .all(Action { component: SoftwareComponent? -> mavenPublication.from(component) })
            })
    }

    companion object {
        /**
         * Name of the task that generates the deployed pom file.
         */
        const val GENERATE_POM_TASK_NAME: String = "generatePomFileForMavenPublication"
    }
}
