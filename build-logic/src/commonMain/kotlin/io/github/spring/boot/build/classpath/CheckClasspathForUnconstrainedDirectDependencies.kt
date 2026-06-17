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
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskAction
import java.util.stream.Collectors

/**
 * Tasks to check that none of classpath's direct dependencies are unconstrained.
 * 
 * @author Andy Wilkinson
 */
abstract class CheckClasspathForUnconstrainedDirectDependencies : DefaultTask() {
    private var classpath: Configuration? = null

    init {
        getOutputs().upToDateWhen(Spec { task: Task? -> true })
    }

    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath!!
    }

    fun setClasspath(classpath: Configuration) {
        this.classpath = classpath
    }

    @TaskAction
    fun checkForUnconstrainedDirectDependencies() {
        val resolutionResult: ResolutionResult = this.classpath!!.getIncoming().getResolutionResult()
        val dependencies: MutableSet<out DependencyResult?> = resolutionResult.getRoot().getDependencies()
        val unconstrainedDependencies = dependencies.stream()
            .map<ComponentSelector> { obj: DependencyResult? -> obj!!.getRequested() }
            .filter { obj: ComponentSelector? -> ModuleComponentSelector::class.java.isInstance(obj) }
            .map<ModuleComponentSelector> { obj: ComponentSelector? -> ModuleComponentSelector::class.java.cast(obj) }
            .map<String> { selector: ModuleComponentSelector? -> selector!!.getGroup() + ":" + selector.getModule() }
            .collect(Collectors.toSet())
        val constraints = resolutionResult.getAllDependencies()
            .stream()
            .filter { obj: DependencyResult? -> obj!!.isConstraint() }
            .map<ComponentSelector> { obj: DependencyResult? -> obj!!.getRequested() }
            .filter { obj: ComponentSelector? -> ModuleComponentSelector::class.java.isInstance(obj) }
            .map<ModuleComponentSelector> { obj: ComponentSelector? -> ModuleComponentSelector::class.java.cast(obj) }
            .map<String> { selector: ModuleComponentSelector? -> selector!!.getGroup() + ":" + selector.getModule() }
            .collect(Collectors.toSet())
        unconstrainedDependencies.removeAll(constraints)
        if (!unconstrainedDependencies.isEmpty()) {
            throw GradleException("Found unconstrained direct dependencies: " + unconstrainedDependencies)
        }
    }
}
