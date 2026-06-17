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

import org.gradle.kotlin.dsl.*

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.util.function.SingletonSupplier
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Check configuration metadata for inconsistencies. The available checks are:
 * 
 *  * Metadata elements [must be sorted alphabetically][.analyzeOrder]
 *  * Metadata elements [must not be duplicates][.analyzeDuplicates]
 *  * Properties [must have a][.analyzePropertyDescription]
 * 
 * 
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesAnalyzer(sources: MutableCollection<File>) {
    private val sources: MutableCollection<File>

    private val jsonMapperSupplier: SingletonSupplier<JsonMapper?>

    init {
        require(!sources.isEmpty()) { "At least one source should be provided" }
        this.sources = sources
        this.jsonMapperSupplier = SingletonSupplier
            .of<JsonMapper?>(Supplier {
                JsonMapper.builder().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION).build()
            })
    }

    fun analyzeOrder(report: Report) {
        for (source in this.sources) {
            report.registerAnalysis(source, analyzeOrder(source))
        }
    }

    private fun analyzeOrder(source: File?): Analysis {
        val json = readJsonContent(source)
        val analysis = Analysis("Metadata element order:")
        for (elementType in ELEMENT_TYPES) {
            analyzeMetadataElementOrder(elementType, json, analysis)
        }
        return analysis
    }

    private fun analyzeMetadataElementOrder(key: String?, json: MutableMap<String?, Any?>, analysis: Analysis) {
        val groups = json.getOrDefault(key, mutableListOf<Any?>()) as MutableList<MutableMap<String?, Any?>?>
        val names = groups.stream().map<String> { group: MutableMap<String?, Any?>? -> group!!.get("name") as String? }
            .toList()
        val sortedNames = names.stream().sorted().toList()
        for (i in names.indices) {
            val actual = names.get(i)
            val expected = sortedNames.get(i)
            if (actual != expected) {
                analysis.addItem(
                    ("Wrong order at $." + key + "[" + i + "].name - expected '" + expected
                            + "' but found '" + actual + "'")
                )
            }
        }
    }

    fun analyzeDuplicates(report: Report) {
        for (source in this.sources) {
            report.registerAnalysis(source, analyzeDuplicates(source))
        }
    }

    private fun analyzeDuplicates(source: File?): Analysis {
        val json = readJsonContent(source)
        val analysis = Analysis("Metadata element duplicates:")
        for (elementType in ELEMENT_TYPES) {
            analyzeMetadataElementDuplicates(elementType, json, analysis)
        }
        return analysis
    }

    private fun analyzeMetadataElementDuplicates(key: String?, json: MutableMap<String?, Any?>, analysis: Analysis) {
        val elements = json.getOrDefault(
            key,
            mutableListOf<Any?>()
        ) as MutableList<MutableMap<String?, Any?>?>
        val names =
            elements.stream().map<String> { group: MutableMap<String?, Any?>? -> group!!.get("name") as String? }
                .toList()
        val uniqueNames: MutableSet<String?> = HashSet<String?>()
        for (i in names.indices) {
            val name = names.get(i)
            if (!uniqueNames.add(name)) {
                analysis.addItem("Duplicate name '" + name + "' at $." + key + "[" + i + "]")
            }
        }
    }

    fun analyzePropertyDescription(report: Report, exclusions: MutableList<String>) {
        for (source in this.sources) {
            report.registerAnalysis(source, analyzePropertyDescription(source, exclusions))
        }
    }

    private fun analyzePropertyDescription(source: File?, exclusions: MutableList<String>): Analysis {
        val json = readJsonContent(source)
        val analysis = Analysis("The following properties have no description:")
        val properties = json.get("properties") as MutableList<MutableMap<String?, Any?>>
        for (property in properties) {
            val name = property.get("name") as String
            if (!isDeprecated(property) && !isDescribed(property) && !isExcluded(exclusions, name)) {
                analysis.addItem(name)
            }
        }
        return analysis
    }

    private fun isExcluded(exclusions: MutableList<String>, propertyName: String): Boolean {
        for (exclusion in exclusions) {
            if (propertyName == exclusion) {
                return true
            }
            if (exclusion.endsWith(".*")) {
                if (propertyName.startsWith(exclusion.substring(0, exclusion.length - 2))) {
                    return true
                }
            }
        }
        return false
    }

    private fun isDeprecated(property: MutableMap<String?, Any?>): Boolean {
        return property.get("deprecation") != null
    }

    private fun isDescribed(property: MutableMap<String?, Any?>): Boolean {
        return property.get("description") != null
    }

    @Throws(IOException::class)
    fun analyzeDeprecationSince(report: Report) {
        for (source in this.sources) {
            report.registerAnalysis(source, analyzeDeprecationSince(source))
        }
    }

    @Throws(IOException::class)
    private fun analyzeDeprecationSince(source: File?): Analysis {
        val analysis = Analysis("The following properties are deprecated without a 'since' version:")
        val json = readJsonContent(source)
        val properties = json.get("properties") as MutableList<MutableMap<String?, Any?>?>
        properties.stream().filter { property: MutableMap<String?, Any?>? -> property!!.containsKey("deprecation") }
            .forEach { property: MutableMap<String?, Any?>? ->
                val deprecation = property!!.get("deprecation") as MutableMap<String?, Any?>
                if (!deprecation.containsKey("since")) {
                    analysis.addItem(property.get("name").toString())
                }
            }
        return analysis
    }

    private fun readJsonContent(source: File?): MutableMap<String?, Any?> {
        @Suppress("UNCHECKED_CAST")
        return this.jsonMapperSupplier.obtain()!!
            .readValue(source, MutableMap::class.java) as MutableMap<String?, Any?>
    }

    class Report(private val baseDirectory: File) {
        private val analyses: MultiValueMap<File, Analysis> = LinkedMultiValueMap<File, Analysis>()

        fun registerAnalysis(path: File, analysis: Analysis?) {
            this.analyses.add(path, analysis)
        }

        fun hasProblems(): Boolean {
            return this.analyses.values.any { candidates ->
                candidates.any { it.hasProblems() }
            }
        }

        fun getAnalyses(source: File?): MutableList<Analysis> {
            return this.analyses.getOrDefault(source, mutableListOf())
        }

        /**
         * Write this report to the given `file`.
         * @param file the file to write the report to
         */
        @Throws(IOException::class)
        fun write(file: File) {
            Files.writeString(
                file.toPath(), createContent(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        }

        private fun createContent(): String? {
            if (this.analyses.isEmpty()) {
                return "No problems found."
            }
            val out = StringWriter()
            PrintWriter(out).use { writer ->
                Companion.writeAll(
                    writer,
                    this.analyses.entries,
                    Consumer { entry ->
                        writer.println(this.baseDirectory.toPath().relativize(entry.key.toPath()))
                        val hasProblems = entry.value.any { it.hasProblems() }
                        if (hasProblems) {
                            Companion.writeAll(
                                writer,
                                entry.value,
                                Consumer { analysis -> analysis.createDetails(writer) })
                        } else {
                            writer.println("No problems found.")
                        }
                    })
            }
            return out.toString()
        }
    }

    class Analysis(private val header: String?) {
        val items: MutableList<String?>

        init {
            this.items = ArrayList<String?>()
        }

        fun addItem(item: String?) {
            this.items.add(item)
        }

        fun hasProblems(): Boolean {
            return !this.items.isEmpty()
        }

        fun createDetails(writer: PrintWriter) {
            writer.println(this.header)
            if (this.items.isEmpty()) {
                writer.println("No problems found.")
            } else {
                for (item in this.items) {
                    writer.println("\t- " + item)
                }
            }
        }

        override fun toString(): String {
            val out = StringWriter()
            val writer = PrintWriter(out)
            createDetails(writer)
            return out.toString()
        }
    }

    companion object {
        private val ELEMENT_TYPES = mutableListOf<String?>("groups", "properties", "hints")

        private fun <T> writeAll(writer: PrintWriter, elements: Iterable<T>, itemWriter: Consumer<T>) {
            val it = elements.iterator()
            while (it.hasNext()) {
                itemWriter.accept(it.next())
                if (it.hasNext()) {
                    writer.println()
                }
            }
        }
    }
}
