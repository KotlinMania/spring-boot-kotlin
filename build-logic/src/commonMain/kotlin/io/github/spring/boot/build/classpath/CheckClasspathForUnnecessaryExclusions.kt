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

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors
import javax.inject.Inject

/**
 * A [Task] for checking the classpath for unnecessary exclusions.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckClasspathForUnnecessaryExclusions @Inject constructor(
    dependencyHandler: DependencyHandler?,
    configurations: ConfigurationContainer?
) : DefaultTask() {
    @get:Input
    val exclusionsByDependencyId: MutableMap<String?, MutableSet<String?>?> = TreeMap<String?, MutableSet<String?>?>()

    private val dependencyById: MutableMap<String?, Dependency> = HashMap<String?, Dependency>()

    private val platform: Dependency

    private val dependencies: DependencyHandler

    private val configurations: ConfigurationContainer

    private var classpath: Configuration? = null

    init {
        this.dependencies = project.getDependencies()
        this.configurations = project.getConfigurations()
        this.platform = this.dependencies
            .create(this.dependencies.platform(this.dependencies.project(SPRING_BOOT_DEPENDENCIES_PROJECT)))
        getOutputs().upToDateWhen(Spec { task: Task? -> true })
    }

    fun setClasspath(classpath: Configuration) {
        this.classpath = classpath
        this.exclusionsByDependencyId.clear()
        this.dependencyById.clear()
        classpath.getAllDependencies().configureEach { processDependency(this) }
    }

    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath!!
    }

    private fun processDependency(dependency: Dependency?) {
        if (dependency is ModuleDependency) {
            processDependency(dependency)
        }
    }

    private fun processDependency(dependency: ModuleDependency) {
        val dependencyId = getId(dependency)
        val exclusions: TreeSet<String?> = dependency.getExcludeRules()
            .stream()
            .map<String> { rule: ExcludeRule? -> this.getId(rule!!) }
            .collect(Collectors.toCollection(Supplier { TreeSet() }))
        this.exclusionsByDependencyId.put(dependencyId, exclusions)
        if (!exclusions.isEmpty()) {
            this.dependencyById.put(dependencyId, this.dependencies.create(dependencyId))
        }
    }

    @TaskAction
    fun checkForUnnecessaryExclusions() {
        val unnecessaryExclusions: MutableMap<String?, MutableSet<String?>?> = HashMap<String?, MutableSet<String?>?>()
        this.exclusionsByDependencyId.forEach { (dependencyId: String?, exclusions: MutableSet<String?>?) ->
            if (!exclusions!!.isEmpty()) {
                val toCheck: Dependency = this.dependencyById.get(dependencyId)!!
                this.configurations.detachedConfiguration(toCheck, this.platform)
                    .getIncoming()
                    .getArtifacts()
                    .getArtifacts()
                    .stream()
                    .map<String> { artifact: ResolvedArtifactResult? -> this.getId(artifact!!) }
                    .forEach { o: String? -> exclusions.remove(o) }
                removeProfileExclusions(dependencyId, exclusions)
                if (!exclusions.isEmpty()) {
                    unnecessaryExclusions.put(dependencyId, exclusions)
                }
            }
        }
        if (!unnecessaryExclusions.isEmpty()) {
            throw GradleException(getExceptionMessage(unnecessaryExclusions))
        }
    }

    private fun removeProfileExclusions(dependencyId: String?, exclusions: MutableSet<String?>) {
        if ("org.xmlunit:xmlunit-core" == dependencyId) {
            exclusions.remove("javax.xml.bind:jaxb-api")
        }
    }

    private fun getExceptionMessage(unnecessaryExclusions: MutableMap<String?, MutableSet<String?>?>): String {
        val message = StringBuilder("Unnecessary exclusions detected:")
        for (entry in unnecessaryExclusions.entries) {
            message.append(String.format("%n    %s", entry.key))
            for (exclusion in entry.value!!) {
                message.append(String.format("%n       %s", exclusion))
            }
        }
        return message.toString()
    }

    private fun getId(artifact: ResolvedArtifactResult): String {
        return getId(artifact.id.getComponentIdentifier() as ModuleComponentIdentifier)
    }

    private fun getId(dependency: ModuleDependency): String {
        return dependency.getGroup() + ":" + dependency.name
    }

    private fun getId(rule: ExcludeRule): String {
        return rule.getGroup() + ":" + rule.getModule()
    }

    private fun getId(identifier: ModuleComponentIdentifier): String {
        return identifier.getGroup() + ":" + identifier.getModule()
    }

    companion object {
        private val SPRING_BOOT_DEPENDENCIES_PROJECT: Map<String, String> =
            mapOf("path" to ":platform:spring-boot-dependencies")
    }
}
