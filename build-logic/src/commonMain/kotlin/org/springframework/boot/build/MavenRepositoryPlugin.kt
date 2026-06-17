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
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.attributes.Category
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.specs.Spec
import org.springframework.util.FileSystemUtils
import java.io.File

/**
 * A plugin to make a project's `deployment` publication available as a Maven
 * repository. The repository can be consumed by depending upon the project using the
 * `mavenRepository` configuration.
 * 
 * @author Andy Wilkinson
 */
class MavenRepositoryPlugin : Plugin<Project?> {
    override fun apply(project: Project) {
        project.getPlugins().apply<MavenPublishPlugin?>(MavenPublishPlugin::class.java)
        val publishing = project.getExtensions().getByType<PublishingExtension>(PublishingExtension::class.java)
        val repositoryLocation = project.getLayout().getBuildDirectory().dir("maven-repository").get().getAsFile()
        publishing.getRepositories().maven(Action { mavenRepository: MavenArtifactRepository? ->
            mavenRepository!!.setName("project")
            mavenRepository.setUrl(repositoryLocation.toURI())
        })
        project.getTasks()
            .matching(Spec { task: Task? -> task!!.getName() == PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME })
            .all(Action { task: Task? -> setUpProjectRepository(project, task!!, repositoryLocation) })
        project.getTasks()
            .matching(Spec { task: Task? -> task!!.getName() == "publishPluginMavenPublicationToProjectRepository" })
            .all(Action { task: Task? -> setUpProjectRepository(project, task!!, repositoryLocation) })
    }

    private fun setUpProjectRepository(project: Project, publishTask: Task, repositoryLocation: File) {
        publishTask.doFirst(CleanAction(repositoryLocation))
        val projectRepository = project.getConfigurations().create(MAVEN_REPOSITORY_CONFIGURATION_NAME)
        project.getArtifacts()
            .add(
                projectRepository.getName(),
                repositoryLocation,
                Action { artifact: ConfigurablePublishArtifact? -> artifact!!.builtBy(publishTask) })
        val target = projectRepository.getDependencies()
        project.getPlugins()
            .withType<JavaPlugin?>(JavaPlugin::class.java)
            .all(Action { javaPlugin: JavaPlugin? ->
                addMavenRepositoryProjectDependencies(
                    project,
                    JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, target
                )
            })
        project.getPlugins()
            .withType<JavaLibraryPlugin?>(JavaLibraryPlugin::class.java)
            .all(Action { javaLibraryPlugin: JavaLibraryPlugin? ->
                addMavenRepositoryProjectDependencies(
                    project,
                    JavaPlugin.API_CONFIGURATION_NAME, target
                )
            })
        project.getPlugins().withType<JavaPlatformPlugin?>(JavaPlatformPlugin::class.java)
            .all(Action { javaPlugin: JavaPlatformPlugin? ->
                addMavenRepositoryProjectDependencies(project, JavaPlatformPlugin.API_CONFIGURATION_NAME, target)
                addMavenRepositoryPlatformDependencies(project, JavaPlatformPlugin.API_CONFIGURATION_NAME, target)
            })
    }

    private fun addMavenRepositoryProjectDependencies(
        project: Project, sourceConfigurationName: String,
        target: DependencySet
    ) {
        project.getConfigurations()
            .getByName(sourceConfigurationName)
            .getDependencies()
            .withType<ProjectDependency?>(ProjectDependency::class.java)
            .all(Action { dependency: ProjectDependency? ->
                val copy = dependency!!.copy()
                if (copy.getAttributes().isEmpty()) {
                    copy.setTargetConfiguration(MAVEN_REPOSITORY_CONFIGURATION_NAME)
                }
                target.add(copy)
            })
    }

    private fun addMavenRepositoryPlatformDependencies(
        project: Project, sourceConfigurationName: String,
        target: DependencySet
    ) {
        project.getConfigurations()
            .getByName(sourceConfigurationName)
            .getDependencies()
            .withType<ModuleDependency?>(ModuleDependency::class.java)
            .matching(Spec { dependency: ModuleDependency? ->
                val category = dependency!!.getAttributes().getAttribute<Category?>(Category.CATEGORY_ATTRIBUTE)
                Category.REGULAR_PLATFORM == category!!.getName()
            })
            .all(Action { dependency: ModuleDependency? ->
                val pom = project.getDependencies()
                    .create(dependency!!.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion())
                target.add(pom)
            })
    }

    private class CleanAction(private val location: File?) : Action<Task?> {
        override fun execute(task: Task?) {
            FileSystemUtils.deleteRecursively(this.location)
        }
    }

    companion object {
        /**
         * Name of the `mavenRepository` configuration.
         */
        const val MAVEN_REPOSITORY_CONFIGURATION_NAME: String = "mavenRepository"

        /**
         * Name of the task that publishes to the project repository.
         */
        const val PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME: String = "publishMavenPublicationToProjectRepository"
    }
}
