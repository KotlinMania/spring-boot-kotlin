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
package org.springframework.boot.build.mavenplugin

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.gradle.api.file.SyncSpec
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.function.Consumer
import java.util.stream.Collectors
import javax.inject.Inject
import org.gradle.api.provider.SetProperty
import org.gradle.api.file.DirectoryProperty

/**
 * [Task] to make Maven binaries available for integration testing.
 * 
 * @author Andy Wilkinson
 */
abstract class PrepareMavenBinaries @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
    archiveOperations: ArchiveOperations
) : DefaultTask() {
    private val binaries: Provider<MutableSet<FileTree>>

    init {
        val configurations = getProject().getConfigurations()
        val dependencies = getProject().getDependencies()
        this.binaries = this.versions.map<MutableSet<FileTree>?>(Transformer { versions: MutableSet<String?>? ->
            versions!!.stream()
                .map<Configuration> { version: String? ->
                    configurations
                        .detachedConfiguration(dependencies.create("org.apache.maven:apache-maven:" + version + ":bin@zip"))
                }
                .map<File> { obj: Configuration? -> obj!!.singleFile }
                .map<FileTree> { zipPath: File? -> archiveOperations.zipTree(zipPath!!) }
                .collect(Collectors.toSet())
        })
    }

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val versions: SetProperty<String>

    @TaskAction
    fun prepareBinaries() {
        this.fileSystemOperations.sync(Action { sync: SyncSpec ->
            sync!!.into(this.outputDir)
            this.binaries.get().forEach(Consumer { sourcePaths: FileTree -> sync.from(sourcePaths) })
        })
    }
}
