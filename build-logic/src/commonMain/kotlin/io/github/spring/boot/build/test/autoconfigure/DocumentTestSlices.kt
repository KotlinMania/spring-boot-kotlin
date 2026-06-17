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
package org.springframework.boot.build.test.autoconfigure

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.springframework.boot.build.test.autoconfigure.TestSliceMetadata.TestSlice
import tools.jackson.databind.json.JsonMapper
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import java.util.function.Consumer
import org.gradle.api.file.RegularFileProperty

/**
 * [Task] used to document test slices.
 * 
 * @author Andy Wilkinson
 */
abstract class DocumentTestSlices : DefaultTask() {
    private var testSliceMetadata: FileCollection? = null

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    var testSlices: FileCollection
        get() = this.testSliceMetadata
        set(testSlices) {
            this.testSliceMetadata = testSlices
        }

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    @Throws(IOException::class)
    fun documentTestSlices() {
        val testSlices = readTestSlices()
        writeTable(testSlices)
    }

    private fun readTestSlices(): MutableMap<String?, MutableList<TestSlice?>?> {
        val testSlices: MutableMap<String?, MutableList<TestSlice?>?> = TreeMap<String?, MutableList<TestSlice?>?>()
        for (metadataFile in this.testSliceMetadata!!) {
            val mapper = JsonMapper.builder().build()
            val metadata = mapper.readValue<TestSliceMetadata>(metadataFile, TestSliceMetadata::class.java)
            val slices: MutableList<TestSlice?> = ArrayList<TestSlice?>(metadata.testSlices)
            Collections.sort<TestSlice?>(
                slices,
                Comparator { s1: TestSlice?, s2: TestSlice? -> s1!!.annotation.compareTo(s2!!.annotation) })
            testSlices.put(metadata.module, slices)
        }
        return testSlices
    }

    @Throws(IOException::class)
    private fun writeTable(testSlicesByModule: MutableMap<String?, MutableList<TestSlice?>?>) {
        val outputFile = this.outputFile.asFile.get()
        outputFile.parentFile.mkdirs()
        PrintWriter(FileWriter(outputFile)).use { writer ->
            writer.println("[cols=\"d,d,a\"]")
            writer.println("|===")
            writer.println("|Module | Test slice | Imported auto-configuration")
            testSlicesByModule.forEach { (module: String?, testSlices: MutableList<TestSlice?>?) ->
                testSlices!!.forEach(
                    Consumer { testSlice: TestSlice? ->
                        writer.println()
                        writer.printf("| `%s`%n", module)
                        writer.printf("| javadoc:%s[format=annotation]%n", testSlice!!.annotation)
                        writer.println("| ")
                        for (importedAutoConfiguration in testSlice.importedAutoConfigurations) {
                            writer.printf("`%s`%n", importedAutoConfiguration)
                        }
                    })
            }
            writer.println("|===")
        }
    }
}
