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
package org.springframework.boot.build.antora

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.springframework.asm.*
import org.springframework.util.function.ThrowingConsumer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException
import java.lang.String
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.function.Consumer
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.MutableSet
import kotlin.collections.dropLastWhile
import kotlin.collections.indices
import kotlin.collections.plus
import kotlin.collections.toTypedArray
import kotlin.plus
import kotlin.sequences.plus
import kotlin.text.StringBuilder
import kotlin.text.contains
import kotlin.text.endsWith
import kotlin.text.get
import kotlin.text.indexOf
import kotlin.text.isEmpty
import kotlin.text.plus
import kotlin.text.replace
import kotlin.text.split
import kotlin.text.substring
import kotlin.text.toRegex
import kotlin.text.trim

/**
 * A task to check `javadoc:[]` macros in Antora source files.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckJavadocMacros : DefaultTask() {
    private val projectRoot: Path

    private var source: FileCollection? = null

    private var classpath: FileCollection? = null

    init {
        this.projectRoot = getProject().getRootDir().toPath()
    }

    @InputFiles
    fun getSource(): FileCollection {
        return this.source!!
    }

    fun setSource(source: FileCollection) {
        this.source = source
    }

    @Optional
    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath!!
    }

    fun setClasspath(classpath: FileCollection) {
        this.classpath = classpath
    }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty?

    @TaskAction
    fun checkJavadocMacros() {
        val availableClasses = indexClasspath()
        val problems: MutableList<String?> = ArrayList<String?>()
        this.source!!.getAsFileTree()
            .filter(Spec { file: File? -> file!!.getName().endsWith(".adoc") })
            .forEach(Consumer { file: File? -> problems.addAll(checkJavadocMacros(file!!, availableClasses)) })
        val outputFile = this.outputDirectory.file("failure-report.txt").get().getAsFile()
        writeReport(problems, outputFile)
        if (!problems.isEmpty()) {
            throw VerificationException("Javadoc macro check failed. See '%s' for details".formatted(outputFile))
        }
    }

    private fun indexClasspath(): MutableSet<String?> {
        val availableClasses =
            StreamSupport.stream<File?>(this.classpath!!.spliterator(), false).flatMap<String?> { root: File? ->
                if (root!!.isFile()) {
                    try {
                        JarFile(root).use { jar ->
                            return@flatMap jar.stream()
                                .map<String?> { obj: JarEntry? -> obj!!.getName() }
                                .filter { entryName: String? -> entryName!!.endsWith(".class") }
                                .map<String?> { className: String? ->
                                    var className = className
                                    if (className!!.startsWith("META-INF/versions/")) {
                                        className = className.substring("META-INF/versions/".length)
                                    }
                                    className = className!!.substring(0, className.length - ".class".length)
                                    className = className.replace('/', '.')
                                    className
                                }
                                .toList()
                                .stream()
                        }
                    } catch (ex: IOException) {
                        throw UncheckedIOException(ex)
                    }
                }
                Stream.empty<String?>()
            }.collect(Collectors.toSet())
        return availableClasses
    }

    private fun checkJavadocMacros(adocFile: File, availableClasses: MutableSet<String?>): MutableList<String?> {
        val problems: MutableList<String?> = ArrayList<String?>()
        val macros = JavadocMacro.Companion.parse(adocFile)
        for (macro in macros) {
            if (!classIsAvailable(macro.className.name, availableClasses)) {
                problems.add(
                    (this.projectRoot.relativize(macro.className.origin.file!!.toPath()).toString() + ":"
                            + macro.className.origin.line + ":" + macro.className.origin.column + ": class "
                            + macro.className.name + " does not exist.")
                )
            } else {
                val anchor = macro.anchor
                if (anchor != null) {
                    if (anchor is MethodAnchor) {
                        val methodMatcher = MethodMatcher(anchor)
                        inputStreamOf(macro.className.name, ThrowingConsumer { stream: InputStream? ->
                            val reader = ClassReader(stream)
                            reader.accept(methodMatcher, 0)
                        })
                        if (!methodMatcher.matched) {
                            problems.add(
                                (this.projectRoot.relativize(macro.anchor.origin.file!!.toPath()).toString() + ":"
                                        + macro.anchor.origin.line + ":" + anchor.origin().column + ": method "
                                        + anchor + " does not exist")
                            )
                        }
                    } else if (anchor is FieldAnchor) {
                        val fieldMatcher = FieldMatcher(anchor)
                        inputStreamOf(macro.className.name, ThrowingConsumer { stream: InputStream? ->
                            val reader = ClassReader(stream)
                            reader.accept(fieldMatcher, 0)
                        })
                        if (!fieldMatcher.matched) {
                            problems.add(
                                (this.projectRoot.relativize(macro.anchor.origin.file!!.toPath()).toString() + ":"
                                        + macro.anchor.origin.line + ":" + anchor.origin().column + ": field "
                                        + anchor.name + " does not exist")
                            )
                        }
                    }
                }
            }
        }
        return problems
    }

    private fun classIsAvailable(className: String, availableClasses: MutableSet<String?>): Boolean {
        if (availableClasses.contains(className)) {
            return true
        }
        if (className.startsWith("java.") || className.startsWith("javax.")) {
            return jdkResourceForClass(className) != null
        }
        return false
    }

    private fun jdkResourceForClass(className: String): URL? {
        return javaClass.getClassLoader().getResource(className.replace(".", "/") + ".class")
    }

    private fun inputStreamOf(className: String, streamHandler: ThrowingConsumer<InputStream?>) {
        for (root in this.classpath!!) {
            if (root.isFile()) {
                try {
                    JarFile(root).use { jar ->
                        val entry = jar.getEntry(className.replace(".", "/") + ".class")
                        if (entry != null) {
                            jar.getInputStream(entry).use { stream ->
                                streamHandler.accept(stream)
                            }
                            return
                        }
                    }
                } catch (ex: IOException) {
                    throw UncheckedIOException(ex)
                }
            }
        }
        val resource = jdkResourceForClass(className)
        if (resource != null) {
            try {
                resource.openStream().use { stream ->
                    streamHandler.accept(stream)
                }
            } catch (ex: IOException) {
                throw UncheckedIOException(ex)
            }
        }
    }

    private fun writeReport(problems: MutableList<String?>, outputFile: File) {
        outputFile.getParentFile().mkdirs()
        val report = StringBuilder()
        if (!problems.isEmpty()) {
            if (problems.size == 1) {
                report.append("Found 1 javadoc macro problem:%n".formatted())
            } else {
                report.append("Found %d javadoc macro problems:%n".formatted(problems.size))
            }
            problems.forEach(Consumer { problem: String? -> report.append("%s%n".formatted(problem)) })
        }
        try {
            Files.writeString(
                outputFile.toPath(), report.toString(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        } catch (ex: IOException) {
            throw UncheckedIOException(ex)
        }
    }

    private class JavadocMacro(private val className: ClassName, private val anchor: JavadocAnchor?) {
        companion object {
            private fun parse(adocFile: File): MutableList<JavadocMacro> {
                val macros: MutableList<JavadocMacro> = ArrayList<JavadocMacro>()
                try {
                    val adocFilePath = adocFile.toPath()
                    val lines = Files.readAllLines(adocFilePath)
                    for (i in lines.indices) {
                        val matcher: Matcher = JAVADOC_MACRO_PATTERN.matcher(lines.get(i))
                        while (matcher.find()) {
                            val classNameOrigin = Origin(adocFile, i + 1, matcher.start(1) + 1)
                            val target = matcher.group(1)
                            var className = target
                            val endOfUrlAttribute = className.indexOf("}/")
                            if (endOfUrlAttribute != -1) {
                                className = className.substring(endOfUrlAttribute + 2)
                            }
                            var anchor: JavadocAnchor? = null
                            val anchorIndex = className.indexOf("#")
                            if (anchorIndex != -1) {
                                anchor = JavadocAnchor.Companion.of(
                                    className.substring(anchorIndex + 1), Origin(
                                        adocFile,
                                        classNameOrigin.line, classNameOrigin.column + anchorIndex + 1
                                    )
                                )
                                className = className.substring(0, anchorIndex)
                            }
                            macros.add(JavadocMacro(ClassName(classNameOrigin, className), anchor))
                        }
                    }
                } catch (ex: IOException) {
                    throw UncheckedIOException(ex)
                }
                return macros
            }
        }
    }

    private class ClassName(private val origin: Origin, private val name: String)

    @JvmRecord
    private data class Origin(val file: File?, val line: Int, val column: Int)

    private abstract class JavadocAnchor protected constructor(private val origin: Origin) {
        fun origin(): Origin {
            return this.origin
        }

        companion object {
            private fun of(anchor: String, origin: Origin): JavadocAnchor {
                var javadocAnchor: JavadocAnchor? = WellKnownAnchor.Companion.of(anchor, origin)
                if (javadocAnchor == null) {
                    javadocAnchor = MethodAnchor.of(anchor, origin)
                }
                if (javadocAnchor == null) {
                    javadocAnchor = FieldAnchor.of(anchor, origin)
                }
                return javadocAnchor
            }
        }
    }

    private class WellKnownAnchor(origin: Origin) : JavadocAnchor(origin) {
        companion object {
            private fun of(anchor: String, origin: Origin): WellKnownAnchor? {
                if (anchor == "enum-constant-summary") {
                    return WellKnownAnchor(origin)
                }
                return null
            }
        }
    }

    private class MethodAnchor(private val name: String?, private val arguments: MutableList<String?>, origin: Origin) :
        JavadocAnchor(origin) {
        override fun toString(): String {
            return this.name + "(" + String.join(", ", this.arguments.toString() + ")")
        }

        companion object {
            fun of(anchor: kotlin.String, origin: Origin): MethodAnchor? {
                if (!anchor.contains("(")) {
                    return null
                }
                val openingIndex = anchor.indexOf('(')
                val name = anchor.substring(0, openingIndex)
                val arguments = Stream.of<kotlin.String>(
                    *anchor.substring(openingIndex + 1, anchor.length - 1).split(",".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                )
                    .map<kotlin.String?> { obj: kotlin.String? -> obj!!.trim { it <= ' ' } }
                    .map<kotlin.String?> { argument: kotlin.String? ->
                        if (argument!!.endsWith("...")) argument.replace(
                            "...",
                            "[]"
                        ) else argument
                    }
                    .toList()
                return MethodAnchor(name, arguments, origin)
            }
        }
    }

    private class FieldAnchor(private val name: kotlin.String?, origin: Origin) : JavadocAnchor(origin) {
        companion object {
            fun of(anchor: kotlin.String?, origin: Origin): FieldAnchor {
                return FieldAnchor(anchor, origin)
            }
        }
    }

    private class MethodMatcher(private val methodAnchor: MethodAnchor) : ClassVisitor(SpringAsmInfo.ASM_VERSION) {
        private var matched = false

        override fun visitMethod(
            access: Int, name: kotlin.String, descriptor: kotlin.String, signature: kotlin.String?,
            exceptions: Array<kotlin.String?>?
        ): MethodVisitor? {
            if (!this.matched && name == this.methodAnchor.name) {
                val type = Type.getType(descriptor)
                if (type.getArgumentCount() == this.methodAnchor.arguments.size) {
                    val argumentTypeNames = Arrays.asList<Type?>(*type.getArgumentTypes())
                        .stream()
                        .map<kotlin.String?> { obj: Type? -> obj!!.getClassName() }
                        .toList()
                    if (argumentTypeNames == this.methodAnchor.arguments) {
                        this.matched = true
                    }
                }
            }
            return null
        }
    }

    private class FieldMatcher(private val fieldAnchor: FieldAnchor) : ClassVisitor(SpringAsmInfo.ASM_VERSION) {
        private var matched = false

        override fun visitField(
            access: Int,
            name: kotlin.String,
            descriptor: kotlin.String?,
            signature: kotlin.String?,
            value: Any?
        ): FieldVisitor? {
            if (!this.matched && name == this.fieldAnchor.name) {
                this.matched = true
            }
            return null
        }
    }

    companion object {
        private val JAVADOC_MACRO_PATTERN: Pattern = Pattern.compile("javadoc:(.*?)\\[(.*?)\\]")
    }
}
