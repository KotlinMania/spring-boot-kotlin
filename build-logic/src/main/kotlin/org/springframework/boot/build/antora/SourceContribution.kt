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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Zip
import org.springframework.boot.build.AntoraConventions
import java.util.concurrent.Callable

/**
 * A contribution of source to Antora.
 * 
 * @author Andy Wilkinson
 */
internal class SourceContribution(project: Project?, name: String?) : Contribution(project, name) {
    fun produce() {
        val antoraSource = getProject().getConfigurations().create(CONFIGURATION_NAME)
        val antoraSourceZip =
            getProject().getTasks().register<Zip?>("antoraSourceZip", Zip::class.java, Action { zip: Zip? ->
                zip!!.getDestinationDirectory().set(getProject().getLayout().getBuildDirectory().dir("antora-source"))
                zip.from(AntoraConventions.ANTORA_SOURCE_DIR)
                zip.setDescription(
                    "Creates a zip archive of the Antora source in %s.".formatted(AntoraConventions.ANTORA_SOURCE_DIR)
                )
            })
        getProject().getArtifacts().add(antoraSource.getName(), antoraSourceZip)
    }

    fun consumeFrom(path: String?) {
        val configuration = createConfiguration(getName())
        val dependencies = getProject().getDependencies()
        dependencies.add(
            configuration.getName(),
            getProject().provider<Dependency?>(Callable { projectDependency(path, CONFIGURATION_NAME) })
        )
        val outputDirectory: Provider<Directory?> = outputDirectory("source", getName())
        val tasks = getProject().getTasks()
        val syncSource = tasks.register<SyncAntoraSource?>(
            taskName("sync", "%s", configuration.getName()),
            SyncAntoraSource::class.java,
            Action { task: SyncAntoraSource? -> configureSyncSource(task!!, path, configuration, outputDirectory) })
        configureAntora(addInputFrom(syncSource, configuration.getName()))
        configurePlaybookGeneration(
            Action { generatePlaybook: GenerateAntoraPlaybook? ->
                generatePlaybook!!.getContentSource().addStartPath(outputDirectory)
            })
    }

    private fun configureSyncSource(
        task: SyncAntoraSource, path: String?, configuration: Configuration?,
        outputDirectory: Provider<Directory?>
    ) {
        task.setDescription("Syncs the %s Antora source from %s.".formatted(getName(), path))
        task.setSource(configuration)
        task.getOutputDirectory().set(outputDirectory)
    }

    private fun createConfiguration(name: String?): Configuration {
        return getProject().getConfigurations().create(configurationName(name, "AntoraSource"))
    }

    companion object {
        private const val CONFIGURATION_NAME = "antoraSource"
    }
}
