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
package org.springframework.boot.build.antora

import org.antora.gradle.AntoraTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.springframework.boot.build.AntoraConventions
import org.springframework.util.StringUtils
import java.util.*
import java.util.Map

/**
 * A contribution to Antora.
 * 
 * @author Andy Wilkinson
 */
internal abstract class Contribution protected constructor(
    protected val project: Project,
    protected val name: String?
) {
    protected fun projectDependency(path: String, configurationName: String): Dependency {
        return this.project.getDependencies()
            .project(Map.of<String?, String?>("path", path, "configuration", configurationName))
    }

    protected fun outputDirectory(dependencyType: String?, theName: String?): Provider<Directory?> {
        return this.project.getLayout()
            .getBuildDirectory()
            .dir("generated/docs/antora-dependencies-" + dependencyType + "/" + theName)
    }

    protected fun taskName(verb: String?, `object`: String, vararg args: String?): String {
        return name(verb, `object`, *args)
    }

    protected fun configurationName(name: String, type: String, vararg args: String?): String {
        return name(toCamelCase(name), type, *args)
    }

    protected fun configurePlaybookGeneration(action: Action<GenerateAntoraPlaybook?>) {
        this.project.getTasks()
            .named<GenerateAntoraPlaybook?>(
                AntoraConventions.GENERATE_ANTORA_PLAYBOOK_TASK_NAME,
                GenerateAntoraPlaybook::class.java,
                action
            )
    }

    protected fun configureAntora(action: Action<AntoraTask?>) {
        this.project.getTasks().named<AntoraTask?>("antora", AntoraTask::class.java, action)
    }

    protected fun addInputFrom(task: TaskProvider<*>, propertyName: String): Action<AntoraTask?> {
        return Action { antora: AntoraTask? ->
            antora!!.getInputs()
                .files(task)
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .withPropertyName(propertyName)
        }
    }

    private fun name(prefix: String?, format: String, vararg args: String?): String {
        return prefix + format.formatted(
            *Arrays.stream<String?>(args).map<String?> { input: String? -> this.toPascalCase(input!!) }.toArray()
        )
    }

    private fun toPascalCase(input: String): String {
        return StringUtils.capitalize(toCamelCase(input))
    }

    private fun toCamelCase(input: String): String {
        val output = StringBuilder(input.length)
        var capitalize = false
        for (c in input.toCharArray()) {
            if (c == '-') {
                capitalize = true
            } else {
                output.append(if (capitalize) c.uppercaseChar() else c)
                capitalize = false
            }
        }
        return output.toString()
    }
}
