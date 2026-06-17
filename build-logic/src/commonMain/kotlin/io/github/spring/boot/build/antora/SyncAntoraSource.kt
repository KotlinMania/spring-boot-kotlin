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
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.function.Consumer
import javax.inject.Inject

/**
 * Task sync Antora source.
 * 
 * @author Andy Wilkinson
 */
abstract class SyncAntoraSource @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations
) : DefaultTask() {
    private var source: FileCollection = null

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @InputFiles
    fun source: FileCollection {
        return this.source!!
    }

    fun setSource(source: FileCollection) {
        this.source = source
    }

    @TaskAction
    fun syncAntoraSource() {
        this.fileSystemOperations.sync(Action { sync: SyncSpec -> this.syncAntoraSource(sync) })
    }

    private fun syncAntoraSource(sync: CopySpec) {
        sync.into(this.outputDirectory)
        this.source!!.files.forEach(Consumer { file: File? -> sync.from(this.archiveOperations.zipTree(file!!)) })
    }
}
