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
package org.springframework.boot.build.starters

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
import org.springframework.boot.build.ConventionsPlugin
import org.springframework.boot.build.DeployedPlugin
import org.springframework.boot.build.classpath.CheckClasspathForConflicts
import org.springframework.boot.build.classpath.CheckClasspathForUnconstrainedDirectDependencies
import org.springframework.boot.build.classpath.CheckClasspathForUnnecessaryExclusions
import org.springframework.util.StringUtils

/**
 * A [Plugin] for a starter project.
 * 
 * @author Andy Wilkinson
 */
class StarterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val plugins = project.getPlugins()
        plugins.apply<DeployedPlugin>(DeployedPlugin::class.java)
        plugins.apply<JavaLibraryPlugin>(JavaLibraryPlugin::class.java)
        plugins.apply<ConventionsPlugin>(ConventionsPlugin::class.java)
        val configurations = project.getConfigurations()
        val runtimeClasspath = configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        val starterMetadata = project.getTasks()
            .register<StarterMetadata>(
                "starterMetadata",
                StarterMetadata::class.java) { task: StarterMetadata ->
                    task!!.setDependencies(runtimeClasspath)
                    val destination: Provider<RegularFile> = project.getLayout()
                        .getBuildDirectory()
                        .file("starter-metadata.properties")
                    task.destination.set(destination)
                }
        configurations.create("starterMetadata")
        project.getArtifacts()
            .add(
                "starterMetadata",
                starterMetadata.map<RegularFileProperty>(Transformer { obj: StarterMetadata? -> obj!!.destination })) { artifact: ConfigurablePublishArtifact -> artifact!!.builtBy(starterMetadata) }
        createClasspathConflictsCheck(runtimeClasspath, project)
        createUnnecessaryExclusionsCheck(runtimeClasspath, project)
        createUnconstrainedDirectDependenciesCheck(runtimeClasspath, project)
        configureJarManifest(project)
    }

    private fun createClasspathConflictsCheck(classpath: Configuration, project: Project) {
        val checkClasspathForConflicts = project.getTasks()
            .register<CheckClasspathForConflicts>(
                "check" + StringUtils.capitalize(classpath.name + "ForConflicts"),
                CheckClasspathForConflicts::class.java) { task: CheckClasspathForConflicts -> task!!.setClasspath(classpath) }
        project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(checkClasspathForConflicts)
    }

    private fun createUnnecessaryExclusionsCheck(classpath: Configuration, project: Project) {
        val checkClasspathForUnnecessaryExclusions = project.getTasks()
            .register<CheckClasspathForUnnecessaryExclusions>(
                "check" + StringUtils.capitalize(classpath.name + "ForUnnecessaryExclusions"),
                CheckClasspathForUnnecessaryExclusions::class.java) { task: CheckClasspathForUnnecessaryExclusions -> task!!.setClasspath(classpath) }
        project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(checkClasspathForUnnecessaryExclusions)
    }

    private fun createUnconstrainedDirectDependenciesCheck(classpath: Configuration, project: Project) {
        val checkClasspathForUnconstrainedDirectDependencies = project
            .getTasks()
            .register<CheckClasspathForUnconstrainedDirectDependencies>(
                "check" + StringUtils.capitalize(classpath.name + "ForUnconstrainedDirectDependencies"),
                CheckClasspathForUnconstrainedDirectDependencies::class.java) { task: CheckClasspathForUnconstrainedDirectDependencies -> task!!.setClasspath(classpath) }
        project.getTasks()
            .getByName(JavaBasePlugin.CHECK_TASK_NAME)
            .dependsOn(checkClasspathForUnconstrainedDirectDependencies)
    }

    private fun configureJarManifest(project: Project) {
        project.getTasks().withType<Jar>(Jar::class.java) { jar: Jar ->
            project.afterEvaluate(Action { evaluated: Project ->
                jar!!.manifest(
                    Action { manifest: Manifest ->
                        val attributes: MutableMap<String?, Any?> = java.util.TreeMap<String?, Any?>()
                        attributes.put("Spring-Boot-Jar-Type", JAR_TYPE)
                        manifest!!.attributes(attributes)
                    })
            })
        }
    }

    companion object {
        private const val JAR_TYPE = "dependencies-starter"
    }
}
