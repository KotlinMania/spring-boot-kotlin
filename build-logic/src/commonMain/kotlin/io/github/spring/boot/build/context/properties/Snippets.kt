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

import org.gradle.api.file.FileCollection
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * Configuration properties snippets.
 * 
 * @author Brian Clozed
 * @author Phillip Webb
 */
class Snippets(configurationPropertyMetadata: FileCollection, private val deprecated: Boolean) {
    private val properties: ConfigurationProperties

    private val snippets: MutableList<Snippet> = mutableListOf()

    init {
        this.properties = ConfigurationProperties.Companion.fromFiles(configurationPropertyMetadata)
    }

    fun add(anchor: String?, title: String?, config: Consumer<Snippet.Config?>?) {
        this.snippets.add(Snippet(anchor, title, config))
    }

    @Throws(IOException::class)
    fun writeTo(outputDirectory: Path) {
        createDirectory(outputDirectory)
        val remaining = this.properties.stream()
            .filter { shouldAdd(it) }
            .map { it!!.name }
            .collect(Collectors.toSet())
        for (snippet in this.snippets) {
            val written = writeSnippet(outputDirectory, snippet, remaining)
            remaining.removeAll(written)
        }
        check(this.deprecated || remaining.isEmpty()) {
            "The following keys were not written to the documentation: " + remaining.joinToString(", ")
        }
    }

    @Throws(IOException::class)
    private fun writeSnippet(
        outputDirectory: Path,
        snippet: Snippet,
        remaining: Set<String>
    ): MutableSet<String> {
        val table = Table()
        val added = mutableSetOf<String>()
        snippet.forEachOverride { prefix, description ->
            val row = CompoundRow(snippet, prefix, if (!this.deprecated) description else "")
            remaining.filter { it.startsWith(prefix!!) }.forEach { name ->
                val property = this.properties.get(name)
                if (shouldAdd(property) && added.add(name)) {
                    row.addProperty(property!!)
                }
            }
            if (!row.isEmpty) {
                table.addRow(row)
            }
        }
        snippet.forEachPrefix { prefix ->
            remaining.filter { it.startsWith(prefix!!) }.forEach { name ->
                val property = this.properties.get(name)
                if (shouldAdd(property) && added.add(name)) {
                    table.addRow(SingleRow(this, snippet, property!!))
                }
            }
        }
        val asciidoc = getAsciidoc(snippet, table)
        writeAsciidoc(outputDirectory, snippet, asciidoc)
        return added
    }

    fun findXref(name: String): String? {
        val property = this.properties.get(name)
        if (property == null || property.isDeprecated) {
            return null
        }
        for (snippet in this.snippets) {
            for (prefix in snippet.overrides.keys) {
                if (name.startsWith(prefix!!)) {
                    return null
                }
            }
            for (prefix in snippet.prefixes) {
                if (name.startsWith(prefix!!)) {
                    return "appendix:application-properties/index.adoc#%s.%s".format(snippet.anchor, name)
                }
            }
        }
        return null
    }

    private fun shouldAdd(property: ConfigurationProperty?): Boolean {
        return (property == null || (property.isDeprecated == this.deprecated && !deprecatedAtErrorLevel(property)))
    }

    private fun deprecatedAtErrorLevel(property: ConfigurationProperty): Boolean {
        val deprecation = property.deprecation
        return deprecation != null && "error" == deprecation.level
    }

    private fun getAsciidoc(snippet: Snippet, table: Table): Asciidoc {
        val asciidoc = Asciidoc()
        if (!table.isEmpty) {
            // We have to prepend 'appendix.' as a section id here, otherwise the
            // spring-asciidoctor-extensions:section-id asciidoctor extension complains
            asciidoc.appendln("[[appendix." + (if (this.deprecated) "deprecated-" else "") + snippet.anchor + "]]")
            asciidoc.appendln("== ", (if (this.deprecated) "Deprecated " else "") + snippet.title)
            table.write(asciidoc)
        }
        return asciidoc
    }

    @Throws(IOException::class)
    private fun writeAsciidoc(outputDirectory: Path, snippet: Snippet, asciidoc: Asciidoc) {
        val parts = (snippet.anchor ?: "").split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val path = outputDirectory.resolve(parts[parts.size - 1] + ".adoc")
        createDirectory(path.getParent())
        Files.deleteIfExists(path)
        Files.newOutputStream(path).use { outputStream ->
            outputStream.write(
                asciidoc.toString().toByteArray(
                    StandardCharsets.UTF_8
                )
            )
        }
    }

    @Throws(IOException::class)
    private fun createDirectory(path: Path) {
        assertValidOutputDirectory(path)
        if (!Files.exists(path)) {
            Files.createDirectory(path)
        }
    }

    private fun assertValidOutputDirectory(path: Path) {
        requireNotNull(path) { "Directory path should not be null" }
        require(!(Files.exists(path) && !Files.isDirectory(path))) { "Path already exists and is not a directory" }
    }
}
