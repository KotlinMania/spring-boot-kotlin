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

import org.gradle.kotlin.dsl.*

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.SyncSpec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.Callable
import javax.inject.Inject
import org.gradle.api.provider.Property
import org.gradle.api.file.DirectoryProperty

/**
 * Tasks for syncing the source code of a Spring Boot application, filtering its
 * `build.gradle` to set the version of its `org.springframework.boot` plugin.
 * 
 * @author Andy Wilkinson
 */
abstract class SyncAppSource @Inject constructor(fileSystemOperations: FileSystemOperations) : DefaultTask() {
    private val fileSystemOperations: FileSystemOperations

    init {
        this.pluginVersion.convention(project.provider<String>(Callable { project.version.toString() }))
        this.fileSystemOperations = fileSystemOperations
    }

    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val destinationDirectory: DirectoryProperty

    @get:Input
    abstract val pluginVersion: Property<String>

    @TaskAction
    fun syncAppSources() {
        this.fileSystemOperations.sync {
            from(this@SyncAppSource.sourceDirectory)
            into(this@SyncAppSource.destinationDirectory)
            filter(Transformer { line: String ->
                line.replace(
                    "id \"org.springframework.boot\"",
                    "id \"org.springframework.boot\" version \"" + this@SyncAppSource.pluginVersion.get() + "\""
                )
            })
        }
    }
}
