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

import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.plugins.ide.api.XmlFileContentMerger
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.*

/**
 * Conventions that are applied in the presence of the [EclipsePlugin] to work
 * around buildship issue `#1238`.
 * 
 * @author Phillip Webb
 */
class EclipseConventions(private val systemRequirements: SystemRequirementsExtension) {
    fun apply(project: Project) {
        project.plugins.withType<EclipsePlugin>().configureEach { configure(project, this) }
        project.afterEvaluate { setJavaRuntimeName(this) }
    }

    private fun configure(project: Project, eclipsePlugin: EclipsePlugin?) {
        val synchronizeResourceSettings = registerEclipseSynchronizeResourceSettings(project)
        val synchronizeJdtSettings: TaskProvider<*> = registerEclipseSynchronizeJdtSettings(project)
        project.plugins.withType<JavaBasePlugin>().configureEach {
            val model = project.getExtensions().getByType<EclipseModel>()
            model.synchronizationTasks(synchronizeResourceSettings, synchronizeJdtSettings)
            model.jdt { this@EclipseConventions.configureJdt(this) }
            model.classpath { this@EclipseConventions.configureClasspath(this) }
        }
    }

    private fun registerEclipseSynchronizeResourceSettings(project: Project): TaskProvider<*> {
        val eclipseSynchronizateResource: TaskProvider<EclipseSynchronizeResourceSettings> = project.getTasks()
            .register<EclipseSynchronizeResourceSettings>(
                "eclipseSynchronizateResourceSettings",
                EclipseSynchronizeResourceSettings::class.java
            )
        eclipseSynchronizateResource.configure {
            setDescription("Synchronizate the Eclipse resource settings file from Buildship.")
            setOutputFile(project.file(".settings/org.eclipse.core.resources.prefs"))
            setInputFile(project.file(".settings/org.eclipse.core.resources.prefs"))
        }
        return eclipseSynchronizateResource
    }

    private fun registerEclipseSynchronizeJdtSettings(project: Project): TaskProvider<EclipseSynchronizeJdtSettings> {
        val taskProvider: TaskProvider<EclipseSynchronizeJdtSettings> = project.getTasks()
            .register<EclipseSynchronizeJdtSettings>(
                "eclipseSynchronizeJdtSettings",
                EclipseSynchronizeJdtSettings::class.java
            )
        taskProvider.configure {
            setDescription("Synchronizate the Eclipse JDT settings file from Buildship.")
            setOutputFile(project.file(".settings/org.eclipse.jdt.core.prefs"))
            setInputFile(project.file(".settings/org.eclipse.jdt.core.prefs"))
        }
        return taskProvider
    }

    private fun configureJdt(jdt: EclipseJdt) {
        jdt.setSourceCompatibility(JavaVersion.toVersion(JavaConventions.Companion.RUNTIME_JAVA_VERSION))
        jdt.setTargetCompatibility(JavaVersion.toVersion(JavaConventions.Companion.RUNTIME_JAVA_VERSION))
    }

    private fun configureClasspath(classpath: EclipseClasspath) {
        classpath.file { this@EclipseConventions.configureClasspathFile(this) }
    }

    private fun configureClasspathFile(merger: XmlFileContentMerger) {
        merger.whenMerged {
            val content = this
            if (content is Classpath) {
                content.getEntries()
                    .removeIf { entry -> isKotlinPluginContributedBuildDirectory(entry) }
            }
        }
    }

    private fun setJavaRuntimeName(project: Project) {
        val model: EclipseModel? = project.getExtensions().findByType<EclipseModel>()
        val jdt = model?.getJdt()
        if (jdt != null) {
            jdt.setJavaRuntimeName("JavaSE-" + this.systemRequirements.java.version)
        }
    }

    private fun isKotlinPluginContributedBuildDirectory(entry: ClasspathEntry?): Boolean {
        return (entry is Library) && isKotlinPluginContributedBuildDirectory(entry.getPath())
                && isTest(entry)
    }

    private fun isKotlinPluginContributedBuildDirectory(path: String): Boolean {
        return path.contains("/main") && (path.contains("/build/classes/") || path.contains("/build/resources/"))
    }

    private fun isTest(library: Library): Boolean {
        val value = library.getEntryAttributes().get("test")
        return (value is String && value.toBoolean())
    }
}
