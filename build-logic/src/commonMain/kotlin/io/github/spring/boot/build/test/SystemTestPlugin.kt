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
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath
import org.gradle.plugins.ide.eclipse.model.EclipseModel

/**
 * A [Plugin] to configure system testing support in a [Project].
 * 
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class SystemTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin>(
            JavaPlugin::class.java) { javaPlugin: JavaPlugin -> configureSystemTesting(project) }
    }

    private fun configureSystemTesting(project: Project) {
        val systemTestSourceSet = createSourceSet(project)
        createTestTask(project, systemTestSourceSet)
        project.plugins
            .withType<EclipsePlugin>(EclipsePlugin::class.java) { eclipsePlugin: EclipsePlugin ->
                val eclipse = project.getExtensions().getByType<EclipseModel>(EclipseModel::class.java)
                eclipse.classpath { classpath: EclipseClasspath ->
                    classpath!!.getPlusConfigurations()
                        .add(
                            project.getConfigurations()
                                .getByName(systemTestSourceSet.getRuntimeClasspathConfigurationName())
                        )
                }
            }
        project.getDependencies()
            .add(systemTestSourceSet.getRuntimeOnlyConfigurationName(), "org.junit.platform:junit-platform-launcher")
    }

    private fun createSourceSet(project: Project): SourceSet {
        val sourceSets: SourceSetContainer =
            project.getExtensions().getByType<JavaPluginExtension>(JavaPluginExtension::class.java).sourceSets
        val systemTestSourceSet = sourceSets.create(SYSTEM_TEST_SOURCE_SET_NAME)
        val mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        systemTestSourceSet
            .setCompileClasspath(systemTestSourceSet.compileClasspath.plus(mainSourceSet.getOutput()))
        systemTestSourceSet
            .setRuntimeClasspath(systemTestSourceSet.getRuntimeClasspath().plus(mainSourceSet.getOutput()))
        return systemTestSourceSet
    }

    private fun createTestTask(project: Project, systemTestSourceSet: SourceSet): TaskProvider<Test> {
        return project.getTasks().register<Test>(SYSTEM_TEST_TASK_NAME, Test::class.java) { task: Test ->
            task!!.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP)
            task.setDescription("Runs system tests.")
            task.setTestClassesDirs(systemTestSourceSet.getOutput().getClassesDirs())
            task.setClasspath(systemTestSourceSet.getRuntimeClasspath())
            task.shouldRunAfter(JavaPlugin.TEST_TASK_NAME)
            if (this.isCi) {
                task.getOutputs().upToDateWhen(NEVER)
                task.getOutputs().doNotCacheIf("System tests are always rerun on CI", Spec { spec: Task? -> true })
            }
        }
    }

    private val isCi: Boolean
        get() = System.getenv("CI").toBoolean()

    companion object {
        private val NEVER = Spec { task: Task? -> false }

        /**
         * Name of the `systemTest` task.
         */
        var SYSTEM_TEST_TASK_NAME: String = "systemTest"

        /**
         * Name of the `systemTest` source set.
         */
        var SYSTEM_TEST_SOURCE_SET_NAME: String = "systemTest"
    }
}
