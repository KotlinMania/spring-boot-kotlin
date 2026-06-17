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
package org.springframework.boot.build.mavenplugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.springframework.boot.build.mavenplugin.PluginXmlParser.Mojo
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import org.gradle.api.provider.MapProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty

/**
 * A [Task] to document the plugin's goals.
 * 
 * @author Andy Wilkinson
 */
abstract class DocumentPluginGoals : DefaultTask() {
    private val parser = PluginXmlParser()

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val goalSections: MapProperty<String?, String>

    @get:InputFile
    abstract val pluginXml: RegularFileProperty

    @TaskAction
    @Throws(IOException::class)
    fun documentPluginGoals() {
        val plugin = this.parser.parse(this.pluginXml.asFile.get())
        writeOverview(plugin)
        for (mojo in plugin.mojos) {
            documentMojo(plugin, mojo)
        }
    }

    @Throws(IOException::class)
    private fun writeOverview(plugin: PluginXmlParser.Plugin) {
        PrintWriter(
            FileWriter(File(this.outputDir.asFile.get(), "overview.adoc"))
        ).use { writer ->
            writer.println("[cols=\"1,3\"]")
            writer.println("|===")
            writer.println("| Goal | Description")
            writer.println()
            for (mojo in plugin.mojos) {
                writer.printf("| xref:%s[%s:%s]%n", goalSectionId(mojo, false), plugin.goalPrefix, mojo.goal)
                writer.printf("| %s%n", mojo.description)
                writer.println()
            }
            writer.println("|===")
        }
    }

    @Throws(IOException::class)
    private fun documentMojo(plugin: PluginXmlParser.Plugin, mojo: Mojo) {
        PrintWriter(
            FileWriter(File(this.outputDir.asFile.get(), mojo.goal + ".adoc"))
        ).use { writer ->
            val sectionId = goalSectionId(mojo, true)
            writer.printf("[[%s]]%n", sectionId)
            writer.printf("= `%s:%s`%n%n", plugin.goalPrefix, mojo.goal)
            writer.printf("`%s:%s:%s`%n", plugin.groupId, plugin.artifactId, plugin.version)
            writer.println()
            writer.println(mojo.description)
            val parameters =
                mojo.parameters.stream().filter { obj: PluginXmlParser.Parameter -> obj.isEditable() }.toList()
            val requiredParameters =
                parameters.stream().filter { obj: PluginXmlParser.Parameter -> obj.isRequired() }.toList()
            val detailsSectionId = sectionId + ".parameter-details"
            if (!requiredParameters.isEmpty()) {
                writer.println()
                writer.println()
                writer.println()
                writer.printf("[[%s.required-parameters]]%n", sectionId)
                writer.println("== Required parameters")
                writer.println()
                writeParametersTable(writer, detailsSectionId, requiredParameters)
            }
            val optionalParameters = parameters.stream()
                .filter { parameter: PluginXmlParser.Parameter -> !parameter.isRequired() }
                .toList()
            if (!optionalParameters.isEmpty()) {
                writer.println()
                writer.println()
                writer.println()
                writer.printf("[[%s.optional-parameters]]%n", sectionId)
                writer.println("== Optional parameters")
                writer.println()
                writeParametersTable(writer, detailsSectionId, optionalParameters)
            }
            writer.println()
            writer.println()
            writer.println()
            writer.printf("[[%s]]%n", detailsSectionId)
            writer.println("== Parameter details")
            writer.println()
            writeParameterDetails(writer, parameters, detailsSectionId)
        }
    }

    private fun goalSectionId(mojo: Mojo, innerReference: Boolean): String {
        val goalSection = this.goalSections.getting(mojo.goal).get()
        checkNotNull(goalSection) { "Goal '" + mojo.goal + "' has not be assigned to a section" }
        val sectionId = goalSection + "." + mojo.goal + "-goal"
        return if (!innerReference) goalSection + "#" + sectionId else sectionId
    }

    private fun writeParametersTable(
        writer: PrintWriter,
        detailsSectionId: String?,
        parameters: MutableList<PluginXmlParser.Parameter>
    ) {
        writer.println("[cols=\"3,2,3\"]")
        writer.println("|===")
        writer.println("| Name | Type | Default")
        writer.println()
        for (parameter in parameters) {
            val name = parameter.name
            writer.printf("| xref:#%s.%s[%s]%n", detailsSectionId, parameterId(name), name)
            writer.printf("| `%s`%n", typeNameToJavadocLink(shortTypeName(parameter.type), parameter.type))
            val defaultValue = parameter.defaultValue
            if (defaultValue != null) {
                writer.printf("| `%s`%n", defaultValue)
            } else {
                writer.println("|")
            }
            writer.println()
        }
        writer.println("|===")
    }

    private fun writeParameterDetails(
        writer: PrintWriter,
        parameters: MutableList<PluginXmlParser.Parameter>,
        sectionId: String?
    ) {
        for (parameter in parameters) {
            val name = parameter.name
            writer.println()
            writer.println()
            writer.printf("[[%s.%s]]%n", sectionId, parameterId(name))
            writer.printf("=== `%s`%n", name)
            writer.println(parameter.description)
            writer.println()
            writer.println("[cols=\"10h,90\"]")
            writer.println("|===")
            writer.println()
            writeDetail(writer, "Name", name)
            writeDetail(writer, "Type", typeNameToJavadocLink(parameter.type))
            writeOptionalDetail(writer, "Default value", parameter.defaultValue)
            writeOptionalDetail(writer, "User property", parameter.userProperty)
            writeOptionalDetail(writer, "Since", parameter.since)
            writer.println("|===")
        }
    }

    private fun parameterId(name: String): String {
        val id = StringBuilder(name.length + 4)
        for (c in name.toCharArray()) {
            if (Character.isLowerCase(c)) {
                id.append(c)
            } else {
                id.append("-")
                id.append(c.lowercaseChar())
            }
        }
        return id.toString()
    }

    private fun writeDetail(writer: PrintWriter, name: String?, value: String?) {
        writer.printf("| %s%n", name)
        writer.printf("| `%s`%n", value)
        writer.println()
    }

    private fun writeOptionalDetail(writer: PrintWriter, name: String?, value: String?) {
        writer.printf("| %s%n", name)
        if (value != null) {
            writer.printf("| `%s`%n", value)
        } else {
            writer.println("|")
        }
        writer.println()
    }

    private fun shortTypeName(name: String): String {
        var name = name
        if (name.lastIndexOf('.') >= 0) {
            name = name.substring(name.lastIndexOf('.') + 1)
        }
        if (name.lastIndexOf('$') >= 0) {
            name = name.substring(name.lastIndexOf('$') + 1)
        }
        return name
    }

    private fun typeNameToJavadocLink(name: String): String? {
        return typeNameToJavadocLink(name, name)
    }

    private fun typeNameToJavadocLink(shortName: String?, name: String): String? {
        if (name.startsWith("org.springframework.boot.maven")) {
            return "xref:maven-plugin:api/java/" + typeNameToJavadocPath(name) + ".html[" + shortName + "]"
        }
        if (name.startsWith("org.springframework.boot")) {
            return "xref:api:java/" + typeNameToJavadocPath(name) + ".html[" + shortName + "]"
        }
        return shortName
    }

    private fun typeNameToJavadocPath(name: String): String {
        return name.replace(".", "/").replace("$", ".")
    }
}
