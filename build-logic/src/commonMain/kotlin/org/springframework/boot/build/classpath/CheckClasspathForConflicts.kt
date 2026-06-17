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
package org.springframework.boot.build.classpath

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.function.*
import java.util.function.Function
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.stream.Collectors

/**
 * A [Task] for checking the classpath for conflicting classes and resources.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckClasspathForConflicts : DefaultTask() {
    private val ignores: MutableList<Predicate<String?>> = ArrayList<Predicate<String?>>()

    private var classpath: FileCollection? = null

    fun setClasspath(classpath: FileCollection) {
        this.classpath = classpath
    }

    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath!!
    }

    @TaskAction
    @Throws(IOException::class)
    fun checkForConflicts() {
        val classpathContents = ClasspathContents()
        for (file in this.classpath!!) {
            if (file.isDirectory()) {
                val root = file.toPath()
                Files.walk(root).use { pathStream ->
                    pathStream.filter { path: Path? -> Files.isRegularFile(path) }
                        .forEach { entry: Path? ->
                            classpathContents.add(
                                root.relativize(entry).toString(),
                                root.toString()
                            )
                        }
                }
            } else {
                JarFile(file).use { jar ->
                    for (entry in Collections.list<JarEntry>(jar.entries())) {
                        if (!entry.isDirectory()) {
                            classpathContents.add(entry.getName(), file.getAbsolutePath())
                        }
                    }
                }
            }
        }
        val conflicts = classpathContents.getConflicts(this.ignores)
        if (!conflicts.isEmpty()) {
            val message = StringBuilder(String.format("Found classpath conflicts:%n"))
            conflicts.forEach { (entry: String?, locations: MutableList<String?>?) ->
                message.append(String.format("    %s%n", entry))
                locations!!.forEach(Consumer { location: String? ->
                    message.append(
                        String.format(
                            "        %s%n",
                            location
                        )
                    )
                })
            }
            throw GradleException(message.toString())
        }
    }

    fun ignore(predicate: Predicate<String?>?) {
        this.ignores.add(predicate!!)
    }

    private class ClasspathContents {
        private val classpathContents = mutableMapOf<String?, MutableList<String?>>()

        fun add(name: String?, source: String?) {
            classpathContents.getOrPut(name) { mutableListOf() }.add(source)
        }

        fun getConflicts(ignores: MutableList<Predicate<String?>>): Map<String?, MutableList<String?>> {
            return classpathContents.entries
                .filter { it.value.size > 1 }
                .filter { canConflict(it.key!!, ignores) }
                .sortedBy { it.key }
                .associate { it.key to it.value }
        }

        fun canConflict(name: String, ignores: MutableList<Predicate<String?>>): Boolean {
            if (name.startsWith("META-INF/")) {
                return false
            }
            for (ignoredName in IGNORED_NAMES) {
                if (name == ignoredName) {
                    return false
                }
            }
            for (ignore in ignores) {
                if (ignore.test(name)) {
                    return false
                }
            }
            return true
        }

        companion object {
            private val IGNORED_NAMES: MutableSet<String?> = HashSet<String?>(
                mutableListOf<String?>(
                    "about.html", "changelog.txt",
                    "LICENSE", "license.txt", "module-info.class", "notice.txt", "readme.txt"
                )
            )
        }
    }
}
