/*
 * Copyright 2025 the original author or authors.
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
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors

/**
 * [Task] that checks aggregated Spring configuration metadata.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckAggregatedSpringConfigurationMetadata : DefaultTask() {
    private var configurationPropertyMetadata: FileCollection? = null

    @get:OutputFile
    abstract val reportLocation: RegularFileProperty?

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getConfigurationPropertyMetadata(): FileCollection {
        return this.configurationPropertyMetadata!!
    }

    fun setConfigurationPropertyMetadata(configurationPropertyMetadata: FileCollection) {
        this.configurationPropertyMetadata = configurationPropertyMetadata
    }

    @TaskAction
    @Throws(IOException::class)
    fun check() {
        val report = createReport()
        val reportFile = this.reportLocation.get().getAsFile()
        Files.write(reportFile.toPath(), report, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        if (report.hasProblems()) {
            throw VerificationException(
                "Problems found in aggregated Spring configuration metadata. See " + reportFile + " for details."
            )
        }
    }

    private fun createReport(): Report {
        val configurationProperties: ConfigurationProperties =
            ConfigurationProperties.Companion.fromFiles(this.configurationPropertyMetadata)
        val propertyNames = configurationProperties.stream()
            .map<String?> { obj: ConfigurationProperty? -> obj!!.getName() }
            .collect(Collectors.toSet())
        val missingReplacement = configurationProperties.stream()
            .filter { obj: ConfigurationProperty? -> obj!!.isDeprecated() }
            .filter { deprecated: ConfigurationProperty? ->
                val replacement = deprecated!!.getDeprecation().replacement
                replacement != null && !propertyNames.contains(replacement)
            }
            .toList()
        return Report(missingReplacement)
    }

    private class Report(private val propertiesWithMissingReplacement: MutableList<ConfigurationProperty?>) :
        Iterable<String?> {
        fun hasProblems(): Boolean {
            return !this.propertiesWithMissingReplacement.isEmpty()
        }

        override fun iterator(): MutableIterator<String?> {
            val lines: MutableList<String?> = ArrayList<String?>()
            if (this.propertiesWithMissingReplacement.isEmpty()) {
                lines.add("No problems found.")
            } else {
                lines.add("The following properties have a replacement that does not exist:")
                lines.add("")
                lines.addAll(
                    this.propertiesWithMissingReplacement.stream()
                        .map<String?> { property: ConfigurationProperty? ->
                            ("\t" + property!!.getName() + " (replacement "
                                    + property.getDeprecation().replacement + ")")
                        }
                        .toList())
            }
            lines.add("")
            return lines.iterator()
        }
    }
}
