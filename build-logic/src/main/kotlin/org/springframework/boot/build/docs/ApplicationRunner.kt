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
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.Jvm
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * [Task] to run an application for the purpose of capturing its output for
 * inclusion in the reference documentation.
 * 
 * @author Andy Wilkinson
 */
abstract class ApplicationRunner : DefaultTask() {
    private var classpath: FileCollection? = null

    init {
        this.applicationJar.convention("/opt/apps/myapp.jar")
    }

    @get:OutputFile
    abstract val output: RegularFileProperty?

    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath!!
    }

    fun setClasspath(classpath: FileCollection) {
        this.classpath = classpath
    }

    @get:Input
    abstract val args: ListProperty<String?>?

    @get:Input
    abstract val mainClass: Property<String?>?

    @get:Input
    abstract val expectedLogging: Property<String>?

    @get:Input
    abstract val normalizations: MapProperty<String?, String?>?

    @get:Input
    abstract val applicationJar: Property<String?>?

    fun normalizeTomcatPort() {
        this.normalizations.put("(Tomcat started on port )[\\d]+( \\(http\\))", "$18080$2")
        this.normalizations.put("(Tomcat initialized with port )[\\d]+( \\(http\\))", "$18080$2")
    }

    fun normalizeLiveReloadPort() {
        this.normalizations.put("(LiveReload server is running on port )[\\d]+", "$135729")
    }

    @TaskAction
    @Throws(IOException::class)
    fun runApplication() {
        val command: MutableList<String?> = ArrayList<String?>()
        val executable = Jvm.current().getExecutable("java")
        command.add(executable.getAbsolutePath())
        command.add("-cp")
        command.add(
            this.classpath!!.getFiles()
                .stream()
                .map<String?> { obj: File? -> obj!!.getAbsolutePath() }
                .collect(Collectors.joining(File.pathSeparator)))
        command.add(this.mainClass.get())
        command.addAll(this.args.get())
        val outputFile = this.output.getAsFile().get()
        val process = ProcessBuilder().redirectOutput(outputFile)
            .redirectError(outputFile)
            .command(command)
            .start()
        awaitLogging(process)
        process.destroy()
        normalizeLogging()
    }

    private fun awaitLogging(process: Process) {
        val end = System.currentTimeMillis() + 60000
        val expectedLogging = this.expectedLogging.get()
        while (System.currentTimeMillis() < end) {
            for (line in outputLines()) {
                if (line.contains(expectedLogging)) {
                    return
                }
            }
            check(process.isAlive()) { "Process exited before '" + expectedLogging + "' was logged" }
        }
        throw IllegalStateException("'" + expectedLogging + "' was not logged within 60 seconds")
    }

    private fun outputLines(): MutableList<String> {
        val outputPath = this.output.get().getAsFile().toPath()
        try {
            return Files.readAllLines(outputPath)
        } catch (ex: IOException) {
            throw RuntimeException("Failed to read lines of output from '" + outputPath + "'", ex)
        }
    }

    private fun normalizeLogging() {
        val outputLines = outputLines()
        val normalizedLines = normalize(outputLines)
        val outputPath = this.output.get().getAsFile().toPath()
        try {
            Files.write(outputPath, normalizedLines)
        } catch (ex: IOException) {
            throw RuntimeException("Failed to write normalized lines of output to '" + outputPath + "'", ex)
        }
    }

    private fun normalize(lines: MutableList<String>): MutableList<String> {
        var normalizedLines = lines
        val normalizations: MutableMap<String?, String?> = HashMap<String?, String?>(
            this.normalizations.get()
        )
        normalizations.put(
            "(Starting .* using Java .* with PID [\\d]+ \\().*( started by ).*( in ).*(\\))",
            "$1" + this.applicationJar.get() + "$2myuser$3/opt/apps/$4"
        )
        for (normalization in normalizations.entries) {
            val pattern = Pattern.compile(normalization.key)
            normalizedLines = normalize(normalizedLines, pattern, normalization.value!!)
        }
        return normalizedLines
    }

    private fun normalize(lines: MutableList<String>, pattern: Pattern, replacement: String): MutableList<String> {
        var matched = false
        val normalizedLines: MutableList<String> = ArrayList<String>()
        for (line in lines) {
            val matcher = pattern.matcher(line)
            val transformed = StringBuilder()
            while (matcher.find()) {
                matched = true
                matcher.appendReplacement(transformed, replacement)
            }
            matcher.appendTail(transformed)
            normalizedLines.add(transformed.toString())
        }
        if (!matched) {
            reportUnmatchedNormalization(lines, pattern)
        }
        return normalizedLines
    }

    private fun reportUnmatchedNormalization(lines: MutableList<String>, pattern: Pattern?) {
        val message = StringBuilder(
            "'" + pattern + "' did not match any of the following lines of output:"
        )
        message.append(String.format("%n"))
        for (line in lines) {
            message.append(String.format("%s%n", line))
        }
        throw IllegalStateException(message.toString())
    }
}
