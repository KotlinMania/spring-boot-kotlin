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

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodeExtension.version
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.npm.task.NpmInstallTask
import io.spring.gradle.antora.GenerateAntoraYmlPlugin
import io.spring.gradle.antora.GenerateAntoraYmlTask
import org.antora.gradle.AntoraPlugin
import org.antora.gradle.AntoraTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.Transformer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.springframework.boot.build.antora.AntoraAsciidocAttributes
import org.springframework.boot.build.antora.CheckJavadocMacros
import org.springframework.boot.build.antora.GenerateAntoraPlaybook
import org.springframework.boot.build.bom.BomExtension
import org.springframework.boot.build.bom.ResolvedBom.Companion.readFrom
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.util.List
import java.util.Map
import java.util.concurrent.Callable

/**
 * Conventions that are applied in the presence of the [AntoraPlugin].
 * 
 * @author Phillip Webb
 */
class AntoraConventions {
    fun apply(project: Project) {
        project.getPlugins().withType<AntoraPlugin?>(
            AntoraPlugin::class.java,
            Action { antoraPlugin: AntoraPlugin? -> apply(project, antoraPlugin) })
    }

    private fun apply(project: Project, antoraPlugin: AntoraPlugin?) {
        val resolvedBom = project.getConfigurations().create("resolveBom")
        project.getDependencies()
            .add(
                resolvedBom.getName(), project.getDependencies()
                    .project(Map.of<String?, String?>("path", DEPENDENCIES_PATH, "configuration", "resolvedBom"))
            )
        project.getPlugins().apply<GenerateAntoraYmlPlugin?>(GenerateAntoraYmlPlugin::class.java)
        val tasks = project.getTasks()
        val generateAntoraPlaybookTask = tasks.register<GenerateAntoraPlaybook?>(
            GENERATE_ANTORA_PLAYBOOK_TASK_NAME, GenerateAntoraPlaybook::class.java,
            Action { task: GenerateAntoraPlaybook? -> configureGenerateAntoraPlaybookTask(project, task!!) })
        val copyAntoraPackageJsonTask = tasks.register<Copy?>(
            "copyAntoraPackageJson", Copy::class.java,
            Action { task: Copy? -> configureCopyAntoraPackageJsonTask(project, task!!) })
        val npmInstallTask = tasks.register<NpmInstallTask?>(
            "antoraNpmInstall", NpmInstallTask::class.java,
            Action { task: NpmInstallTask? -> configureNpmInstallTask(project, task!!, copyAntoraPackageJsonTask) })
        tasks.withType<GenerateAntoraYmlTask?>(
            GenerateAntoraYmlTask::class.java,
            Action { generateAntoraYmlTask: GenerateAntoraYmlTask? ->
                configureGenerateAntoraYmlTask(
                    project,
                    generateAntoraYmlTask!!,
                    resolvedBom
                )
            })
        tasks.withType<AntoraTask?>(
            AntoraTask::class.java,
            Action { antoraTask: AntoraTask? ->
                configureAntoraTask(
                    project,
                    antoraTask!!,
                    npmInstallTask,
                    generateAntoraPlaybookTask
                )
            })
        project.getExtensions()
            .configure<NodeExtension?>(
                NodeExtension::class.java,
                Action { nodeExtension: NodeExtension? -> configureNodeExtension(project, nodeExtension!!) })
        val checkAntoraJavadocMacros = tasks.register<CheckJavadocMacros?>(
            "checkAntoraJavadocMacros",
            CheckJavadocMacros::class.java, Action { task: CheckJavadocMacros? ->
                task!!.setSource(project.files(ANTORA_SOURCE_DIR))
                task.outputDirectory.set(project.getLayout().getBuildDirectory().dir(task.getName()))
            })
        project.getPlugins().withType<JavaPlugin?>(JavaPlugin::class.java, Action { java: JavaPlugin? ->
            val runtimeClasspathConfigurationName: String = project.getExtensions()
                .getByType<JavaPluginExtension?>(JavaPluginExtension::class.java)
                .getSourceSets()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                .getRuntimeClasspathConfigurationName()
            val javadocMacros =
                project.getConfigurations().create("javadocMacros", Action { configuration: Configuration ->
                    configuration.extendsFrom(project.getConfigurations().getByName(runtimeClasspathConfigurationName))
                    configuration.setDescription(
                        "Dependencies referenced in javadoc macros. Extends from " + runtimeClasspathConfigurationName
                    )
                    configuration.setCanBeResolved(true)
                    configuration.setCanBeDeclared(true)
                    configuration.setCanBeConsumed(false)
                })
            checkAntoraJavadocMacros.configure(Action { macrosTask: CheckJavadocMacros? ->
                macrosTask!!.setClasspath(
                    javadocMacros
                )
            })
        })
        project.getPlugins()
            .withType<NodePlugin?>(
                NodePlugin::class.java,
                Action { node: NodePlugin? ->
                    project.getExtensions().getByType<NodeExtension?>(NodeExtension::class.java).version.set("24.14.1")
                })
    }

    private fun configureGenerateAntoraPlaybookTask(
        project: Project,
        generateAntoraPlaybookTask: GenerateAntoraPlaybook
    ) {
        val nodeProjectDir: Provider<Directory?> = getNodeProjectDir(project)
        generateAntoraPlaybookTask.outputFile
            .set(nodeProjectDir.map<S?>(Transformer { directory: Directory? -> directory!!.file("antora-playbook.yml") }))
    }

    private fun configureCopyAntoraPackageJsonTask(project: Project, copyAntoraPackageJsonTask: Copy) {
        copyAntoraPackageJsonTask
            .from(
                project.getRootProject().file("antora"),
                Action { spec: CopySpec? -> spec!!.include("package.json", "package-lock.json", "patches/**") })
            .into(getNodeProjectDir(project))
    }

    private fun configureNpmInstallTask(
        project: Project?, npmInstallTask: NpmInstallTask,
        copyAntoraPackageJson: TaskProvider<Copy?>
    ) {
        npmInstallTask.dependsOn(copyAntoraPackageJson)
        val environment: MutableMap<String?, String?> = HashMap<String?, String?>()
        environment.put("npm_config_omit", "optional")
        environment.put("npm_config_update_notifier", "false")
        npmInstallTask.environment.set(environment)
        npmInstallTask.npmCommand.set(mutableListOf<String?>("ci", "--silent", "--no-progress"))
    }

    private fun configureGenerateAntoraYmlTask(
        project: Project, generateAntoraYmlTask: GenerateAntoraYmlTask,
        resolvedBom: Configuration
    ) {
        generateAntoraYmlTask.getOutputs()
            .doNotCacheIf("getAsciidocAttributes() changes output", Spec { task: Task? -> true })
        generateAntoraYmlTask.dependsOn(resolvedBom)
        generateAntoraYmlTask.setProperty("componentName", "boot")
        generateAntoraYmlTask.setProperty(
            "outputFile",
            project.getLayout().getBuildDirectory().file("generated/docs/antora-yml/antora.yml")
        )
        generateAntoraYmlTask.setProperty("yml", getDefaultYml(project))
        generateAntoraYmlTask.getAsciidocAttributes().putAll(getAsciidocAttributes(project, resolvedBom))
    }

    private fun getDefaultYml(project: Project): MutableMap<String?, *> {
        var navFile: String? = null
        for (candidate in NAV_FILES) {
            if (project.file(ANTORA_SOURCE_DIR + "/" + candidate).exists()) {
                Assert.state(navFile == null, "Multiple nav files found")
                navFile = candidate
            }
        }
        val defaultYml: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        defaultYml.put("title", "Spring Boot")
        if (navFile != null) {
            defaultYml.put("nav", List.of<String?>(navFile))
        }
        return defaultYml
    }

    private fun getAsciidocAttributes(
        project: Project,
        resolvedBoms: FileCollection
    ): Provider<MutableMap<String?, String?>?> {
        return project.provider<MutableMap<String?, String?>?>(Callable {
            val bom = project.project(DEPENDENCIES_PATH).getExtensions().getByName("bom") as BomExtension
            val resolvedBom = readFrom(resolvedBoms.getSingleFile())
            AntoraAsciidocAttributes(project, bom, resolvedBom!!).get()
        })
    }

    private fun configureAntoraTask(
        project: Project, antoraTask: AntoraTask,
        npmInstallTask: TaskProvider<NpmInstallTask?>,
        generateAntoraPlaybookTask: TaskProvider<GenerateAntoraPlaybook?>?
    ) {
        antoraTask.setGroup("Documentation")
        antoraTask.dependsOn(npmInstallTask, generateAntoraPlaybookTask!!)
        antoraTask.setPlaybook("antora-playbook.yml")
        antoraTask.setUiBundleUrl(getUiBundleUrl(project))
        antoraTask.args.set(project.provider<MutableList<String?>?>(Callable { getAntoraNpxArs(project, antoraTask) }))
        project.getPlugins()
            .withType<JavaBasePlugin?>(
                JavaBasePlugin::class.java,
                Action { javaBasePlugin: JavaBasePlugin? ->
                    project.getTasks()
                        .getByName(JavaBasePlugin.CHECK_TASK_NAME)
                        .dependsOn(antoraTask)
                })
    }

    private fun getAntoraNpxArs(project: Project, antoraTask: AntoraTask): MutableList<String?> {
        logWarningIfNodeModulesInUserHome(project)
        val startParameter = project.getGradle().getStartParameter()
        val showStacktrace = startParameter.getShowStacktrace().name.startsWith("ALWAYS")
        val debugLogging = project.getGradle().getStartParameter().getLogLevel() == LogLevel.DEBUG
        val playbookPath = antoraTask.getPlaybook()
        val arguments: MutableList<String?> = ArrayList<String?>()
        arguments.addAll(mutableListOf<String?>("--package", "@antora/cli"))
        arguments.add("antora")
        arguments.addAll(if (!showStacktrace) mutableListOf<String?>() else mutableListOf<String?>("--stacktrace"))
        arguments.addAll(
            if (!debugLogging) mutableListOf<String?>("--quiet") else mutableListOf<String?>(
                "--log-level",
                "all"
            )
        )
        arguments.addAll(List.of<String?>("--ui-bundle-url", antoraTask.getUiBundleUrl()))
        arguments.add(playbookPath)
        return arguments
    }

    private fun logWarningIfNodeModulesInUserHome(project: Project) {
        if (File(System.getProperty("user.home"), "node_modules").exists()) {
            project.getLogger()
                .warn(
                    "Detected the existence of \$HOME/node_modules. This directory is "
                            + "not compatible with this plugin. Please remove it."
                )
        }
    }

    private fun getUiBundleUrl(project: Project): String {
        val packageJson = project.getRootProject().file("antora/package.json")
        val jsonMapper = JsonMapper()
        val json = jsonMapper.readerFor(MutableMap::class.java).readValue<MutableMap<*, *>?>(packageJson)
        val config = if (json != null) json.get("config") as MutableMap<*, *>? else null
        val url: String = (if (config != null) config.get("ui-bundle-url") as kotlin.String? else null)!!
        Assert.state(StringUtils.hasText(url.toString()), "package.json has not ui-bundle-url config")
        return url
    }

    private fun configureNodeExtension(project: Project, nodeExtension: NodeExtension) {
        nodeExtension.workDir.set(project.getLayout().getBuildDirectory().dir(".gradle/nodejs"))
        nodeExtension.npmWorkDir.set(project.getLayout().getBuildDirectory().dir(".gradle/npm"))
        nodeExtension.nodeProjectDir.set(getNodeProjectDir(project))
    }

    private fun getNodeProjectDir(project: Project): Provider<Directory?> {
        return project.getLayout().getBuildDirectory().dir(".gradle/nodeproject")
    }

    companion object {
        private const val DEPENDENCIES_PATH = ":platform:spring-boot-dependencies"

        private val NAV_FILES = mutableListOf<String?>("nav.adoc", "local-nav.adoc")

        /**
         * Default Antora source directory.
         */
        const val ANTORA_SOURCE_DIR: String = "src/docs/antora"

        /**
         * Name of the [GenerateAntoraPlaybook] task.
         */
        const val GENERATE_ANTORA_PLAYBOOK_TASK_NAME: String = "generateAntoraPlaybook"
    }
}
