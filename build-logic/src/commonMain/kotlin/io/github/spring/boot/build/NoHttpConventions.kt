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
package org.springframework.boot.build

import org.gradle.kotlin.dsl.*

import io.spring.nohttp.gradle.NoHttpCheckstylePlugin
import io.spring.nohttp.gradle.NoHttpExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle

/**
 * Conventions that are applied to enforce that no HTTP urls are used.
 * 
 * @author Phillip Webb
 */
class NoHttpConventions {
    fun apply(project: Project) {
        project.getPluginManager().apply(NoHttpCheckstylePlugin::class.java)
        configureNoHttpExtension(
            project,
            project.getExtensions().getByType<NoHttpExtension>(NoHttpExtension::class.java)
        )
        project.getTasks()
            .named<Checkstyle>(NoHttpCheckstylePlugin.CHECKSTYLE_NOHTTP_TASK_NAME)
            .configure {
                getConfigDirectory().set(project.getRootProject().file("config/nohttp"))
            }
    }

    private fun configureNoHttpExtension(project: Project, extension: NoHttpExtension) {
        extension.setAllowlistFile(project.getRootProject().file("config/nohttp/allowlist.lines"))
        val source = extension.source
        source.exclude("bin/**")
        source.exclude("build/**")
        source.exclude("out/**")
        source.exclude("target/**")
        source.exclude(".settings/**")
        source.exclude(".classpath")
        source.exclude(".project")
        source.exclude(".gradle")
        source.exclude("**/docker/export.tar")
    }
}
