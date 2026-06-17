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

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.plugin.DetektPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.DokkaExternalDocumentationLinkSpec
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.formats.DokkaHtmlPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.nio.file.Path

/**
 * Conventions that are applied in the presence of the `org.jetbrains.kotlin.jvm`
 * plugin. When the plugin is applied:
 * 
 * 
 *  * [KotlinCompile] tasks are configured to:
 * 
 *  * Use `apiVersion` and `languageVersion` 1.7.
 *  * Use `jvmTarget` 17.
 *  * Treat all warnings as errors
 *  * Suppress version warnings
 * 
 *  * Detekt plugin is applied to perform static analysis of Kotlin code
 * 
 * 
 * 
 * 
 * 
 * @author Andy Wilkinson
 */
class KotlinConventions {
    fun apply(project: Project) {
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.getTasks().withType<KotlinCompile>().configureEach { configure(this) }
            project.plugins.withType<DokkaHtmlPlugin>().configureEach { configureDokka(project) }
            configureDetekt(project)
        }
    }

    private fun configure(compile: KotlinCompile) {
        val compilerOptions = compile.compilerOptions
        compilerOptions.apiVersion.set(KOTLIN_VERSION)
        compilerOptions.languageVersion.set(KOTLIN_VERSION)
        compilerOptions.jvmTarget.set(JVM_TARGET)
        compilerOptions.allWarningsAsErrors.set(true)
        compilerOptions.freeCompilerArgs
            .addAll("-Xsuppress-version-warnings", "-Xannotation-default-target=param-property")
    }

    private fun configureDokka(project: Project) {
        val dokka = project.getExtensions().getByType<DokkaExtension>(DokkaExtension::class.java)
        dokka.dokkaSourceSets.configureEach {
            val sourceSet = this
            if (SourceSet.MAIN_SOURCE_SET_NAME == sourceSet.name) {
                sourceSet.sourceRoots.setFrom(project.file("src/commonMain/kotlin"))
                sourceSet.classpath
                    .from(
                        project.getExtensions()
                            .getByType<SourceSetContainer>()
                            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                            .getOutput()
                    )
                sourceSet.externalDocumentationLinks.create("spring-boot-javadoc") {
                    url.set(URI.create("https://docs.spring.io/spring-boot/api/java/"))
                    packageListUrl
                        .set(URI.create("https://docs.spring.io/spring-boot/api/java/element-list"))
                }
                sourceSet.externalDocumentationLinks.create("spring-framework-javadoc") {
                    val docUrl = "https://docs.spring.io/spring-framework/docs/%s/javadoc-api/"
                        .format(project.property("springFrameworkVersion"))
                    url.set(URI.create(docUrl))
                    packageListUrl.set(URI.create(docUrl + "/element-list"))
                }
            } else {
                sourceSet.suppress.set(true)
            }
        }
    }

    private fun configureDetekt(project: Project) {
        project.plugins.apply<DetektPlugin>(DetektPlugin::class.java)
        val detekt = project.getExtensions().getByType<DetektExtension>(DetektExtension::class.java)
        detekt.config.setFrom(project.getRootProject().file("config/detekt/config.yml"))
        project.getTasks().withType<Detekt>().configureEach {
            jvmTarget.set(JVM_TARGET.target)
            normalizeMachineSpecificDefaults(project, this)
        }
    }

    private fun normalizeMachineSpecificDefaults(project: Project?, task: Detekt) {
        // See: https://github.com/detekt/detekt/issues/7170
        task.basePath.set(pathRelativeToRootProject(task.project).toString())
    }

    companion object {
        private val JVM_TARGET = JvmTarget.JVM_17

        private val KOTLIN_VERSION = KotlinVersion.KOTLIN_2_2

        private fun pathRelativeToRootProject(project: Project): Path {
            val rootProjectDirectory = project.getIsolated().getRootProject().getProjectDirectory().asFile.toPath()
            val projectDirectory = project.getLayout().getProjectDirectory().asFile.toPath()
            return projectDirectory.relativize(rootProjectDirectory)
        }
    }
}
