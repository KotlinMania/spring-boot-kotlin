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

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

/**
 * Tasks to copy Antora content.
 * 
 * @author Andy Wilkinson
 */
abstract class CopyAntoraContent @Inject constructor() : DefaultTask() {
    private var source: FileCollection? = null

    @InputFiles
    fun getSource(): FileCollection {
        return this.source!!
    }

    fun setSource(source: FileCollection) {
        this.source = source
    }

    @get:OutputFile
    abstract val outputFile: RegularFileProperty?

    @TaskAction
    @Throws(IllegalStateException::class, IOException::class)
    fun copyAntoraContent() {
        val source = this.source!!.getSingleFile().toPath()
        val target = this.outputFile.getAsFile().get().toPath()
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    }
}
