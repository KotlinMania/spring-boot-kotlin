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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath
import org.gradle.plugins.ide.eclipse.model.EclipseModel

/**
 * Plugin for Docker-based tests. Creates a [source set][SourceSet], [ test][Test] task, and [shared service][BuildService] named `dockerTest`. The build
 * service is configured to only allow serial usage and the `dockerTest` task is
 * configured to use the build service. In a parallel build, this ensures that only a
 * single `dockerTest` task can run at any given time.
 * 
 * @author Andy Wilkinson
 */
class DockerTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin>(
            JavaPlugin::class.java) { javaPlugin: JavaPlugin -> configureDockerTesting(project) }
    }

    private fun configureDockerTesting(project: Project) {
        val buildService: Provider<DockerTestBuildService> = DockerTestBuildService.registerIfNecessary(project)
        val dockerTestSourceSet = createSourceSet(project)
        val dockerTest: Provider<Test> = createTestTask(project, dockerTestSourceSet, buildService)
        project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(dockerTest)
        project.plugins
            .withType<EclipsePlugin>(EclipsePlugin::class.java) { eclipsePlugin: EclipsePlugin ->
                val eclipse = project.getExtensions().getByType<EclipseModel>(EclipseModel::class.java)
                eclipse.classpath(Action { classpath: EclipseClasspath ->
                    classpath!!.getPlusConfigurations()
                        .add(
                            project.getConfigurations()
                                .getByName(dockerTestSourceSet.getRuntimeClasspathConfigurationName())
                        )
                })
            }
        project.getDependencies()
            .add(dockerTestSourceSet.getRuntimeOnlyConfigurationName(), "org.junit.platform:junit-platform-launcher")
        val reclaimDockerSpace: Provider<Exec> = createReclaimDockerSpaceTask(project, buildService)
        project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(reclaimDockerSpace)
    }

    private fun createSourceSet(project: Project): SourceSet {
        val sourceSets: SourceSetContainer =
            project.getExtensions().getByType<JavaPluginExtension>(JavaPluginExtension::class.java).sourceSets
        val dockerTestSourceSet = sourceSets.create(DOCKER_TEST_SOURCE_SET_NAME)
        val main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
        dockerTestSourceSet.setCompileClasspath(
            dockerTestSourceSet.compileClasspath
                .plus(main.getOutput())
                .plus(main.compileClasspath)
                .plus(test.getOutput())
        )
        dockerTestSourceSet.setRuntimeClasspath(
            dockerTestSourceSet.getRuntimeClasspath()
                .plus(main.getOutput())
                .plus(main.getRuntimeClasspath())
                .plus(test.getOutput())
        )
        project.plugins.withType<IntegrationTestPlugin>(
            IntegrationTestPlugin::class.java) { integrationTestPlugin: IntegrationTestPlugin ->
                val intTest = sourceSets.getByName(IntegrationTestPlugin.Companion.INT_TEST_SOURCE_SET_NAME)
                dockerTestSourceSet
                    .setCompileClasspath(dockerTestSourceSet.compileClasspath.plus(intTest.getOutput()))
                dockerTestSourceSet
                    .setRuntimeClasspath(dockerTestSourceSet.getRuntimeClasspath().plus(intTest.getOutput()))
            }
        return dockerTestSourceSet
    }

    private fun createTestTask(
        project: Project, dockerTestSourceSet: SourceSet,
        buildService: Provider<DockerTestBuildService>
    ): Provider<Test> {
        return project.getTasks().register<Test>(DOCKER_TEST_TASK_NAME, Test::class.java) { task: Test ->
            task!!.usesService(buildService)
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP)
            task.setDescription("Runs Docker-based tests.")
            task.setTestClassesDirs(dockerTestSourceSet.getOutput().getClassesDirs())
            task.setClasspath(dockerTestSourceSet.getRuntimeClasspath())
            task.shouldRunAfter(JavaPlugin.TEST_TASK_NAME)
        }
    }

    private fun createReclaimDockerSpaceTask(
        project: Project,
        buildService: Provider<DockerTestBuildService>
    ): Provider<Exec> {
        return project.getTasks()
            .register<Exec>(RECLAIM_DOCKER_SPACE_TASK_NAME, Exec::class.java) { task: Exec ->
                task!!.usesService(buildService)
                task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP)
                task.setDescription("Reclaims Docker space on CI.")
                task.shouldRunAfter(DOCKER_TEST_TASK_NAME)
                task.onlyIf(Spec { task: Task? -> this.shouldReclaimDockerSpace(task) })
                task.executable("bash")
                task.args(
                    "-c",
                    project.getRootDir()
                        .toPath()
                        .resolve(".github/scripts/reclaim-docker-diskspace.sh")
                        .toAbsolutePath()
                )
            }
    }

    private fun shouldReclaimDockerSpace(task: Task?): Boolean {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return false
        }
        return System.getenv("GITHUB_ACTIONS") != null || System.getenv("RECLAIM_DOCKER_SPACE") != null
    }

    companion object {
        /**
         * Name of the `dockerTest` task.
         */
        const val DOCKER_TEST_TASK_NAME: String = "dockerTest"

        /**
         * Name of the `dockerTest` source set.
         */
        const val DOCKER_TEST_SOURCE_SET_NAME: String = "dockerTest"

        /**
         * Name of the `dockerTest` shared service.
         */
        const val DOCKER_TEST_SERVICE_NAME: String = "dockerTest"

        private const val RECLAIM_DOCKER_SPACE_TASK_NAME = "reclaimDockerSpace"
    }
}
