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
package org.springframework.boot.build.starters

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.springframework.core.CollectionFactory
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.Callable
import java.util.stream.Collectors

/**
 * A [Task] for generating metadata that describes a starter.
 * 
 * @author Andy Wilkinson
 */
abstract class StarterMetadata : DefaultTask() {
    private var dependencies: Configuration? = null

    init {
        val project = getProject()
        this.starterName.convention(project.provider<String?>(Callable { project.getName() }))
        this.starterDescription.convention(project.provider<String?>(Callable { project.getDescription() }))
    }

    @get:Input
    abstract val starterName: Property<String?>?

    @get:Input
    abstract val starterDescription: Property<String?>?

    @Classpath
    fun getDependencies(): FileCollection {
        return this.dependencies!!
    }

    fun setDependencies(dependencies: Configuration) {
        this.dependencies = dependencies
    }

    @get:OutputFile
    abstract val destination: RegularFileProperty?

    @TaskAction
    @Throws(IOException::class)
    fun generateMetadata() {
        val properties = CollectionFactory.createSortedProperties(true)
        properties.setProperty("name", this.starterName.get())
        properties.setProperty("description", this.starterDescription.get())
        properties.setProperty(
            "dependencies",
            java.lang.String.join(
                ",",
                this.dependencies!!.getResolvedConfiguration()
                    .getResolvedArtifacts()
                    .stream()
                    .map<String?> { obj: ResolvedArtifact? -> obj!!.getName() }
                    .collect(Collectors.toSet())))
        val destination = this.destination.getAsFile().get()
        destination.getParentFile().mkdirs()
        FileWriter(destination).use { writer ->
            properties.store(writer, null)
        }
    }
}
