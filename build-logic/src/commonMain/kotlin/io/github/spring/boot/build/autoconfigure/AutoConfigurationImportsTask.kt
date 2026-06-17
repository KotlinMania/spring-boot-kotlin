/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.boot.build.autoconfigure

import org.gradle.kotlin.dsl.*

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.util.PatternFilterable
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import org.gradle.api.file.FileTree

/**
 * A [Task] that uses a project's auto-configuration imports.
 * 
 * @author Andy Wilkinson
 */
abstract class AutoConfigurationImportsTask : DefaultTask() {
    private var sourceFiles: FileCollection = project.getObjects().fileCollection()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:InputFiles
    var source: FileTree
        get() = this.sourceFiles.getAsFileTree()
            .matching { filter: PatternFilterable ->
                filter!!.include(IMPORTS_FILE)
            }
        set(source) {
            this.sourceFiles = project.getObjects().fileCollection().from(source)
        }

    protected fun loadImports(): MutableList<String?> {
        val importsFile = this.source.singleFile
        try {
            return Files.readAllLines(importsFile.toPath())
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }

    companion object {
        /**
         * The path of the `AutoConfiguration.imports` file.
         */
        const val IMPORTS_FILE: String =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
    }
}
