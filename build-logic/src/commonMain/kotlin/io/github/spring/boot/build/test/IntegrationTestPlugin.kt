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
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath
import org.gradle.plugins.ide.eclipse.model.EclipseModel

/**
 * A [Plugin] to configure integration testing support in a [Project].
 * 
 * @author Andy Wilkinson
 */
class IntegrationTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.getPlugins().withType<JavaPlugin>(
            JavaPlugin::class.java,
            Action { javaPlugin: JavaPlugin -> configureIntegrationTesting(project) })
    }

    private fun configureIntegrationTesting(project: Project) {
        val intTestSourceSet = createSourceSet(project)
        val intTest: TaskProvider<Test> = createTestTask(project, intTestSourceSet)
        project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(intTest)
        project.getPlugins()
            .withType<EclipsePlugin>(EclipsePlugin::class.java, Action { eclipsePlugin: EclipsePlugin ->
                val eclipse = project.getExtensions().getByType<EclipseModel>(EclipseModel::class.java)
                eclipse.classpath(Action { classpath: EclipseClasspath ->
                    classpath!!.getPlusConfigurations()
                        .add(
                            project.getConfigurations()
                                .getByName(intTestSourceSet.getRuntimeClasspathConfigurationName())
                        )
                })
            })
        project.getDependencies()
            .add(intTestSourceSet.getRuntimeOnlyConfigurationName(), "org.junit.platform:junit-platform-launcher")
    }

    private fun createSourceSet(project: Project): SourceSet {
        val sourceSets: SourceSetContainer =
            project.getExtensions().getByType<JavaPluginExtension>(JavaPluginExtension::class.java).sourceSets
        val intTestSourceSet = sourceSets.create(INT_TEST_SOURCE_SET_NAME)
        val main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        intTestSourceSet.setCompileClasspath(intTestSourceSet.compileClasspath.plus(main.getOutput()))
        intTestSourceSet.setRuntimeClasspath(intTestSourceSet.getRuntimeClasspath().plus(main.getOutput()))
        return intTestSourceSet
    }

    private fun createTestTask(project: Project, intTestSourceSet: SourceSet): TaskProvider<Test> {
        return project.getTasks().register<Test>(INT_TEST_TASK_NAME, Test::class.java, Action { task: Test ->
            task!!.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP)
            task.setDescription("Runs integration tests.")
            task.setTestClassesDirs(intTestSourceSet.getOutput().getClassesDirs())
            task.setClasspath(intTestSourceSet.getRuntimeClasspath())
            task.shouldRunAfter(JavaPlugin.TEST_TASK_NAME)
        })
    }

    companion object {
        /**
         * Name of the `intTest` task.
         */
        var INT_TEST_TASK_NAME: String = "intTest"

        /**
         * Name of the `intTest` source set.
         */
        var INT_TEST_SOURCE_SET_NAME: String = "intTest"
    }
}
