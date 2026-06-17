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

import org.gradle.kotlin.dsl.*

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors
import org.gradle.api.provider.SetProperty

/**
 * A [Task] for checking the classpath for prohibited dependencies.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckClasspathForProhibitedDependencies : DefaultTask() {
    private var classpath: Configuration? = null

    init {
        getOutputs().upToDateWhen(Spec { task: Task? -> true })
    }

    @get:Input
    abstract val permittedGroups: SetProperty<String>

    fun setClasspath(classpath: Configuration) {
        this.classpath = classpath
    }

    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath!!
    }

    @TaskAction
    fun checkForProhibitedDependencies() {
        val prohibited: TreeSet<String?> = this.classpath!!.getResolvedConfiguration()
            .getResolvedArtifacts()
            .stream()
            .map<ModuleVersionIdentifier> { artifact: ResolvedArtifact? -> artifact!!.getModuleVersion().id }
            .filter { id: ModuleVersionIdentifier? -> this.prohibited(id!!) }
            .map<String> { id: ModuleVersionIdentifier? -> id!!.getGroup() + ":" + id.name }
            .collect(Collectors.toCollection(Supplier { TreeSet() }))
        if (!prohibited.isEmpty()) {
            val message = StringBuilder(String.format("Found prohibited dependencies:%n"))
            for (dependency in prohibited) {
                message.append(String.format("    %s%n", dependency))
            }
            throw GradleException(message.toString())
        }
    }

    private fun prohibited(id: ModuleVersionIdentifier): Boolean {
        return (!this.permittedGroups.get().contains(id.getGroup())) && (PROHIBITED_GROUPS.contains(id.getGroup())
                || prohibitedJavax(id) || prohibitedSlf4j(id) || prohibitedJbossSpec(id))
    }

    private fun prohibitedSlf4j(id: ModuleVersionIdentifier): Boolean {
        return id.getGroup() == "org.slf4j" && id.name == "jcl-over-slf4j"
    }

    private fun prohibitedJbossSpec(id: ModuleVersionIdentifier): Boolean {
        return id.getGroup().startsWith("org.jboss.spec")
    }

    private fun prohibitedJavax(id: ModuleVersionIdentifier): Boolean {
        return id.getGroup().startsWith("javax.") && !PERMITTED_JAVAX_GROUPS.contains(id.getGroup())
    }

    companion object {
        private val PROHIBITED_GROUPS = mutableSetOf<String?>(
            "org.codehaus.groovy", "org.eclipse.jetty.toolchain",
            "org.apache.geronimo.specs", "com.sun.activation"
        )

        private val PERMITTED_JAVAX_GROUPS = mutableSetOf<String?>("javax.batch", "javax.cache", "javax.money")
    }
}
