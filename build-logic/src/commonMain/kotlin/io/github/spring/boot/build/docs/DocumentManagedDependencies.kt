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
import org.springframework.boot.build.bom.ResolvedBom.Bom
import org.springframework.boot.build.bom.ResolvedBom.Companion.readFrom
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import org.gradle.api.file.RegularFileProperty

/**
 * Task for documenting [boms&#39;][ResolvedBom] managed dependencies.
 * 
 * @author Andy Wilkinson
 */
abstract class DocumentManagedDependencies : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    var resolvedBoms: FileCollection? = null

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    @Throws(IOException::class)
    fun documentConstrainedVersions() {
        val outputFile = this.outputFile.get().asFile
        outputFile.parentFile.mkdirs()
        PrintWriter(FileWriter(outputFile)).use { writer ->
            writer.println("|===")
            writer.println("| Group ID | Artifact ID | Version")
            val managedCoordinates: MutableSet<ResolvedBom.Id> =
                TreeSet<ResolvedBom.Id>(Comparator { id1: ResolvedBom.Id, id2: ResolvedBom.Id ->
                    val result = id1.groupId!!.compareTo(id2.groupId!!)
                    if (result != 0) {
                        return@Comparator result
                    }
                    id1.artifactId!!.compareTo(id2.artifactId!!)
                })
            for (file in this.resolvedBoms!!.files) {
                managedCoordinates.addAll(process(readFrom(file)!!))
            }
            for (id in managedCoordinates) {
                writer.println()
                writer.printf("| `%s`%n", id.groupId)
                writer.printf("| `%s`%n", id.artifactId)
                writer.printf("| `%s`%n", id.version)
            }
            writer.println("|===")
        }
    }

    private fun process(resolvedBom: ResolvedBom): MutableSet<ResolvedBom.Id?> {
        val managedCoordinates = TreeSet<ResolvedBom.Id?>()
        for (library in resolvedBom.libraries!!) {
            for (managedDependency in library!!.managedDependencies!!) {
                managedCoordinates.add(managedDependency)
            }
            for (importedBom in library.importedBoms!!) {
                managedCoordinates.addAll(process(importedBom!!))
            }
        }
        return managedCoordinates
    }

    private fun process(bom: Bom): MutableSet<ResolvedBom.Id?> {
        val managedCoordinates = TreeSet<ResolvedBom.Id?>()
        bom.managedDependencies!!.stream().forEach { e: ResolvedBom.Id? -> managedCoordinates.add(e) }
        val parent = bom.parent
        if (parent != null) {
            managedCoordinates.addAll(process(parent))
        }
        return managedCoordinates
    }
}
