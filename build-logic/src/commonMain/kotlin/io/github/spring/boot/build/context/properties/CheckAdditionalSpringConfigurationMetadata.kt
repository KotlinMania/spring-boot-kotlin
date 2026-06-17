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
package org.springframework.boot.build.context.properties

import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import java.io.File
import java.io.IOException
import org.gradle.api.file.RegularFileProperty

/**
 * [SourceTask] that checks additional Spring configuration metadata files.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckAdditionalSpringConfigurationMetadata : SourceTask() {
    private val projectDir: File

    init {
        this.projectDir = getProject().projectDir
    }

    @get:OutputFile
    abstract val reportLocation: RegularFileProperty

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree {
        return super.source
    }

    @TaskAction
    @Throws(IOException::class)
    fun check() {
        val analyzer = ConfigurationPropertiesAnalyzer(getSource().files)
        val report = ConfigurationPropertiesAnalyzer.Report(this.projectDir)
        analyzer.analyzeOrder(report)
        analyzer.analyzeDuplicates(report)
        analyzer.analyzeDeprecationSince(report)
        val reportFile = this.reportLocation.get().asFile
        report.write(reportFile)
        if (report.hasProblems()) {
            throw VerificationException(
                "Problems found in additional Spring configuration metadata. See " + reportFile + " for details."
            )
        }
    }
}
