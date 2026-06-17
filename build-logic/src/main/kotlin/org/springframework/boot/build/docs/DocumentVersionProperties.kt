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
package org.springframework.boot.build.docs

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.springframework.boot.build.bom.ResolvedBom
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

/**
 * Task for documenting [boms&#39;][ResolvedBom] version properties.
 * 
 * @author Christoph Dreis
 * @author Andy Wilkinson
 */
abstract class DocumentVersionProperties : DefaultTask() {
    private var resolvedBoms: FileCollection? = null

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getResolvedBoms(): FileCollection {
        return this.resolvedBoms!!
    }

    fun setResolvedBoms(resolvedBoms: FileCollection) {
        this.resolvedBoms = resolvedBoms
    }

    @get:OutputFile
    abstract val outputFile: RegularFileProperty?

    @TaskAction
    @Throws(IOException::class)
    fun documentVersionProperties() {
        val libraries = this.resolvedBoms!!.getFiles()
            .stream()
            .map<ResolvedBom?> { obj: File? -> ResolvedBom.Companion.readFrom() }
            .flatMap<ResolvedBom.ResolvedLibrary?> { resolvedBom: ResolvedBom? -> resolvedBom!!.libraries!!.stream() }
            .sorted { l1: ResolvedBom.ResolvedLibrary, l2: ResolvedBom.ResolvedLibrary ->
                l1.name!!.compareTo(
                    l2.name!!,
                    ignoreCase = true
                )
            }
            .toList()
        val outputFile = this.outputFile.getAsFile().get()
        outputFile.getParentFile().mkdirs()
        PrintWriter(FileWriter(outputFile)).use { writer ->
            writer.println("|===")
            writer.println("| Library | Version Property")
            for (library in libraries) {
                writer.println()
                writer.printf("| `%s`%n", library.name)
                writer.printf("| `%s`%n", library.versionProperty)
            }
            writer.println("|===")
        }
    }
}
