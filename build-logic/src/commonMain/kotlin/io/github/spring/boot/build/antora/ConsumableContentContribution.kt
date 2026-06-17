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
package org.springframework.boot.build.antora

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.Transformer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import java.util.concurrent.Callable

/**
 * A contribution of content to Antora that can be consumed by other projects.
 * 
 * @author Andy Wilkinson
 */
open class ConsumableContentContribution protected constructor(
    project: Project?,
    type: String?,
    name: String?
) : ContentContribution(project, name, type) {
    override fun produceFrom(copySpec: CopySpec?) {
        this.produceFrom(copySpec, false)
    }

    fun produceFrom(copySpec: CopySpec?, publish: Boolean) {
        val producer = super.configureProduction(copySpec)
        if (publish) {
            publish(producer)
        }
        val configuration = createConfiguration(
            getName(),
            "Configuration for %s Antora %s content artifacts."
        )
        configuration.setCanBeConsumed(true)
        configuration.setCanBeResolved(false)
        getProject().getArtifacts().add(configuration.name, producer)
    }

    fun consumeFrom(path: String?) {
        val configuration = createConfiguration(getName(), "Configuration for %s Antora %s content.")
        configuration.setCanBeConsumed(false)
        configuration.setCanBeResolved(true)
        val dependencies = getProject().getDependencies()
        dependencies.add(
            configuration.name,
            getProject().provider<Dependency>(Callable { projectDependency(path, configuration.name) })
        )
        val outputDirectory: Provider<Directory> = outputDirectory("content", getName())
        val tasks = getProject().getTasks()
        val copyAntoraContent: TaskProvider<*> = tasks.register<CopyAntoraContent>(
            taskName("copy", "%s", configuration.name),
            CopyAntoraContent::class.java) { task: CopyAntoraContent -> configureCopyContent(task!!, path, configuration, outputDirectory) }
        configureAntora(addInputFrom(copyAntoraContent, configuration.name))
        configurePlaybookGeneration(Action { task: GenerateAntoraPlaybook ->
            this.addToZipContentsCollectorDependencies(
                task
            )
        })
        publish(copyAntoraContent)
    }

    fun publish(producer: TaskProvider<out Task?>) {
        getProject().getExtensions()
            .getByType<PublishingExtension>(PublishingExtension::class.java)
            .publications
            .withType<MavenPublication>(MavenPublication::class.java)
            .configureEach(Action { mavenPublication: MavenPublication ->
                addPublishedMavenArtifact(
                    mavenPublication!!,
                    producer
                )
            })
    }

    private fun configureCopyContent(
        task: CopyAntoraContent, path: String?, configuration: Configuration?,
        outputDirectory: Provider<Directory>
    ) {
        task.setDescription(
            "Syncs the %s Antora %s content from %s.".format(getName(), toDescription(getType()), path)
        )
        task.setSource(configuration)
        task.outputFile
            .set(outputDirectory.map<RegularFile>(Transformer { dir: Directory? -> this.getContentZipFile(dir!!) }))
    }

    private fun addToZipContentsCollectorDependencies(task: GenerateAntoraPlaybook) {
        task.antoraExtensions.getZipContentsCollector().getDependencies().add(getName())
    }

    private fun addPublishedMavenArtifact(mavenPublication: MavenPublication, producer: TaskProvider<*>) {
        if ("maven" == mavenPublication.name) {
            val classifier: String = "%s-%s-content".format(getName(), getType())
            mavenPublication.artifact(
                producer) { mavenArtifact: MavenArtifact -> mavenArtifact!!.setClassifier(classifier) }
        }
    }

    private fun getContentZipFile(dir: Directory): RegularFile {
        val version = getProject().version
        return dir.file("spring-boot-docs-%s-%s-%s-content.zip".format(version, getName(), getType()))
    }

    private fun createConfiguration(name: String?, description: String): Configuration {
        return getProject().getConfigurations()
            .create(
                configurationName(name, "Antora%sContent", getType())) { configuration: Configuration ->
                    configuration.setDescription(
                        description.format(
                            getName(),
                            getType()
                        )
                    )
                }
    }

    companion object {
        private fun toDescription(input: String): String {
            return input.replace("-", " ")
        }
    }
}
