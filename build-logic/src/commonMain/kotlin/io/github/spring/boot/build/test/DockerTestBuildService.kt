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
package org.springframework.boot.build.test

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec

/**
 * Build service for Docker-based tests. The maximum number of `dockerTest` tasks
 * that can run in parallel can be configured using
 * `org.springframework.boot.dockertest.max-parallel-tasks`. By default, only a
 * single `dockerTest` task will run at a time.
 * 
 * @author Andy Wilkinson
 */
object DockerTestBuildService : BuildService<BuildServiceParameters.None?> {
    fun registerIfNecessary(project: Project): Provider<DockerTestBuildService> {
        return project.getGradle()
            .getSharedServices()
            .registerIfAbsent<DockerTestBuildService?, BuildServiceParameters.None?>(
                "dockerTest", DockerTestBuildService::class.java,
                Action { spec: BuildServiceSpec<BuildServiceParameters.None?> ->
                    spec!!.getMaxParallelUsages().set(
                        maxParallelTasks(project)
                    )
                })
    }

    private fun maxParallelTasks(project: Project): Int {
        val property = project.findProperty("org.springframework.boot.dockertest.max-parallel-tasks")
        if (property == null) {
            return 1
        }
        return property.toString().toInt()
    }
}
