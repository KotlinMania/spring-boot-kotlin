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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationException
import java.io.File
import java.io.IOException
import java.util.List
import org.gradle.api.provider.ListProperty
import org.gradle.api.file.RegularFileProperty

/**
 * [SourceTask] that checks `spring-configuration-metadata.json` files.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckSpringConfigurationMetadata : DefaultTask() {
    private val projectRoot: File

    init {
        this.projectRoot = getProject().projectDir
    }

    @get:OutputFile
    abstract val reportLocation: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val metadataLocation: RegularFileProperty

    @get:Input
    abstract val exclusions: ListProperty<String>

    @TaskAction
    @Throws(IOException::class)
    fun check() {
        val report = ConfigurationPropertiesAnalyzer.Report(this.projectRoot)
        val analyzer = ConfigurationPropertiesAnalyzer(
            List.of<File?>(this.metadataLocation.get().asFile)
        )
        analyzer.analyzePropertyDescription(report, this.exclusions.get())
        val reportFile = this.reportLocation.get().asFile
        report.write(reportFile)
        if (report.hasProblems()) {
            throw VerificationException(
                "Problems found in Spring configuration metadata. See " + reportFile + " for details."
            )
        }
    }
}
