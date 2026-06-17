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

import com.gradle.develocity.agent.gradle.test.DevelocityTestConfiguration
import com.gradle.develocity.agent.gradle.test.PredictiveTestSelectionConfiguration
import com.gradle.develocity.agent.gradle.test.TestRetryConfiguration
import io.spring.gradle.nullability.NullabilityPlugin
import io.spring.gradle.nullability.NullabilityPluginExtension
import io.spring.javaformat.gradle.SpringJavaFormatPlugin
import io.spring.javaformat.gradle.tasks.CheckFormat
import io.spring.javaformat.gradle.tasks.Format
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileTreeElement
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.jvm.toolchain.JavaCompiler
import org.gradle.jvm.toolchain.JavaInstallationMetadata
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.springframework.boot.build.architecture.ArchitecturePlugin
import org.springframework.boot.build.classpath.CheckClasspathForProhibitedDependencies
import org.springframework.boot.build.optional.OptionalDependenciesPlugin
import org.springframework.boot.build.springframework.CheckAotFactories
import org.springframework.boot.build.springframework.CheckSpringFactories
import org.springframework.boot.build.testing.TestFailuresPlugin
import org.springframework.boot.build.toolchain.ToolchainPlugin
import org.springframework.util.StringUtils
import java.util.*
import java.util.List
import java.util.stream.Collectors

/**
 * Conventions that are applied in the presence of the [JavaBasePlugin]. When the
 * plugin is applied:
 * 
 * 
 *  * The project is configured with source and target compatibility of 17
 *  * [Spring Java Format][SpringJavaFormatPlugin], [ Checkstyle][CheckstylePlugin], [Test Failures][TestFailuresPlugin], [ Architecture][ArchitecturePlugin] and [NullabilityPlugin] plugins are applied
 *  * [Test] tasks are configured:
 * 
 *  * to use JUnit Platform
 *  * with a max heap of 1536M
 *  * to run after any Checkstyle and format checking tasks
 *  * to enable retries with a maximum of three attempts when running on CI
 *  * to use predictive test selection when the value of the
 * `ENABLE_PREDICTIVE_TEST_SELECTION` environment variable is `true`
 * 
 *  * A `testRuntimeOnly` dependency upon
 * `org.junit.platform:junit-platform-launcher` is added to projects with the
 * [JavaPlugin] applied
 *  * [JavaCompile], [Javadoc], and [Format] tasks are configured to
 * use UTF-8 encoding
 *  * [JavaCompile] tasks are configured to:
 * 
 *  * Use `-parameters`.
 *  * Treat warnings as errors
 *  * Enable `unchecked`, `deprecation`, `rawtypes`, and
 * `varargs` warnings
 * 
 *  * [Jar] tasks are configured to produce jars with LICENSE.txt and NOTICE.txt
 * files and the following manifest entries:
 * 
 *  * `Automatic-Module-Name`
 *  * `Build-Jdk-Spec`
 *  * `Built-By`
 *  * `Implementation-Title`
 *  * `Implementation-Version`
 * 
 *  * `spring-boot-parent` is used for dependency management
 *  * Additional checks are configured:
 * 
 *  * For all source sets:
 * 
 *  * Prohibited dependencies on the compile classpath
 *  * Prohibited dependencies on the runtime classpath
 * 
 *  * For the `main` source set:
 * 
 *  * `META-INF/spring/aot.factories`
 *  * `META-INF/spring.factories`
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author Mike Smithson
 * @author Scott Frederick
 */
class JavaConventions(private val systemRequirements: SystemRequirementsExtension) {
    fun apply(project: Project) {
        project.plugins.withType<JavaBasePlugin>(JavaBasePlugin::class.java) { java: JavaBasePlugin ->
            project.plugins.apply<TestFailuresPlugin>(TestFailuresPlugin::class.java)
            project.plugins.apply<ArchitecturePlugin>(ArchitecturePlugin::class.java)
            configureSpringJavaFormat(project)
            configureJavaConventions(project)
            configureJavadocConventions(project)
            configureTestConventions(project)
            configureJarManifestConventions(project)
            configureDependencyManagement(project)
            configureToolchain(project)
            configureProhibitedDependencyChecks(project)
            configureFactoriesFilesChecks(project)
            configureNullability(project)
        }
    }

    private fun configureJarManifestConventions(project: Project) {
        val extractLegalResources = project.getTasks()
            .register<ExtractResources>(
                "extractLegalResources",
                ExtractResources::class.java) { task: ExtractResources ->
                    task!!.packageName.set("org.springframework.boot.build.legal")
                    task.destinationDirectory.set(project.getLayout().getBuildDirectory().dir("legal"))
                    task.resourceNames
                        .set(kotlin.collections.mutableListOf<String?>("LICENSE.txt", "NOTICE.txt"))
                    task.properties.put("version", project.version.toString())
                }
        val sourceSets = project.getExtensions().getByType<SourceSetContainer>(SourceSetContainer::class.java)
        val sourceJarTaskNames = sourceSets.stream()
            .map<String> { obj: SourceSet? -> obj!!.getSourcesJarTaskName() }
            .collect(Collectors.toSet())
        val javadocJarTaskNames = sourceSets.stream()
            .map<String> { obj: SourceSet? -> obj!!.getJavadocJarTaskName() }
            .collect(Collectors.toSet())
        project.getTasks().withType<Jar>(Jar::class.java) { jar: Jar ->
            project.afterEvaluate(Action { evaluated: Project ->
                jar!!.metaInf(Action { metaInf: CopySpec -> metaInf!!.from(extractLegalResources) })
                jar.manifest(Action { manifest: Manifest ->
                    val attributes: MutableMap<String?, Any?> = java.util.TreeMap<String?, Any?>()
                    attributes.put("Automatic-Module-Name", project.name.replace("-", "."))
                    // Build-Jdk-Spec is used by buildpacks to pick the JRE to install
                    attributes.put("Build-Jdk-Spec", this.systemRequirements.java.version)
                    attributes.put("Built-By", "Spring")
                    attributes.put(
                        "Implementation-Title",
                        determineImplementationTitle(project, sourceJarTaskNames, javadocJarTaskNames, jar)
                    )
                    attributes.put("Implementation-Version", project.version)
                    manifest!!.attributes(attributes)
                })
            })
        }
    }

    private fun determineImplementationTitle(
        project: Project, sourceJarTaskNames: MutableSet<String?>,
        javadocJarTaskNames: MutableSet<String?>, jar: Jar
    ): String? {
        if (sourceJarTaskNames.contains(jar.name)) {
            return "Source for " + project.name
        }
        if (javadocJarTaskNames.contains(jar.name)) {
            return "Javadoc for " + project.name
        }
        return project.description
    }

    private fun configureTestConventions(project: Project) {
        project.getTasks().withType<Test>(Test::class.java) { test: Test ->
            test!!.useJUnitPlatform()
            test.setMaxHeapSize("1536M")
            project.getTasks().withType<Checkstyle>(
                Checkstyle::class.java) { paths: Checkstyle -> test.mustRunAfter(paths!!) }
            project.getTasks().withType<CheckFormat>(
                CheckFormat::class.java) { paths: CheckFormat -> test.mustRunAfter(paths!!) }
            configureTestRetries(test)
            configurePredictiveTestSelection(test)
        }
        project.plugins
            .withType<JavaPlugin>(JavaPlugin::class.java) { javaPlugin: JavaPlugin ->
                project.getDependencies()
                    .add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.platform:junit-platform-launcher")
            }
    }

    private fun configureTestRetries(test: Test) {
        val testRetry: TestRetryConfiguration = test.getExtensions()
            .getByType<DevelocityTestConfiguration>(DevelocityTestConfiguration::class.java)
            .testRetry
        testRetry.getFailOnPassedAfterRetry().set(false)
        testRetry.getMaxRetries().set(if (this.isCi) 3 else 0)
    }

    private val isCi: Boolean
        get() = System.getenv("CI").toBoolean()

    private fun configurePredictiveTestSelection(test: Test) {
        if (this.isPredictiveTestSelectionEnabled) {
            val predictiveTestSelection: PredictiveTestSelectionConfiguration = test.getExtensions()
                .getByType<DevelocityTestConfiguration>(DevelocityTestConfiguration::class.java)
                .predictiveTestSelection
            predictiveTestSelection.getEnabled().convention(true)
        }
    }

    private val isPredictiveTestSelectionEnabled: Boolean
        get() = System.getenv("ENABLE_PREDICTIVE_TEST_SELECTION").toBoolean()

    private fun configureJavadocConventions(project: Project) {
        project.getTasks().withType<Javadoc>(Javadoc::class.java) { javadoc: Javadoc ->
            val options = javadoc!!.getOptions() as CoreJavadocOptions
            options.source("17")
            options.encoding("UTF-8")
            addValuelessOption(options, "Xdoclint:none")
            addValuelessOption(options, "quiet")
            if (!javadoc.name.contains("aggregated")) {
                addValuelessOption(options, "-no-fonts")
            }
        }
    }

    private fun addValuelessOption(options: CoreJavadocOptions, option: String?) {
        options.addMultilineMultiValueOption(option).setValue(List.of<MutableList<String?>?>(mutableListOf<String?>()))
    }

    private fun configureJavaConventions(project: Project) {
        project.getTasks().withType<JavaCompile>(JavaCompile::class.java) { compile: JavaCompile ->
            compile!!.doFirst(Action { task: Task -> assertCompatible(compile) })
            compile.getOptions().setEncoding("UTF-8")
            compile.getOptions().getRelease().set(RUNTIME_JAVA_VERSION)
            val args: MutableSet<String?> = java.util.LinkedHashSet<String?>(compile.getOptions().getCompilerArgs())
            args.addAll(
                kotlin.collections.mutableListOf<String?>(
                    "-parameters", "-Werror", "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:rawtypes",
                    "-Xlint:varargs"
                )
            )
            compile.getOptions().setCompilerArgs(java.util.ArrayList<String?>(args))
        }
    }

    private fun assertCompatible(compile: JavaCompile) {
        val requiredVersion = JavaVersion.toVersion(BUILD_JAVA_VERSION)
        val actualVersion = compile.getJavaCompiler()
            .map<JavaInstallationMetadata>(Transformer { obj: JavaCompiler? -> obj!!.getMetadata() })
            .map<JavaLanguageVersion>(Transformer { obj: JavaInstallationMetadata? -> obj!!.getLanguageVersion() })
            .map<Int>(Transformer { obj: JavaLanguageVersion? -> obj!!.asInt() })
            .map<JavaVersion>(Transformer { value: Int? -> JavaVersion.toVersion(value!!) })
            .orElse(JavaVersion.current())
            .get()
        if (!actualVersion.isCompatibleWith(requiredVersion)) {
            throw GradleException("This project should be built with Java %s or above".format(requiredVersion))
        }
    }

    private fun configureSpringJavaFormat(project: Project) {
        project.plugins.apply<SpringJavaFormatPlugin>(SpringJavaFormatPlugin::class.java)
        project.getTasks()
            .withType<Format>(Format::class.java) { Format: Format -> Format!!.setEncoding("UTF-8") }
        project.plugins.apply<CheckstylePlugin>(CheckstylePlugin::class.java)
        val checkstyle = project.getExtensions().getByType<CheckstyleExtension>(CheckstyleExtension::class.java)
        val checkstyleToolVersion = project.findProperty("checkstyleToolVersion") as String?
        checkstyle.setToolVersion(checkstyleToolVersion!!)
        checkstyle.getConfigDirectory().set(project.getRootProject().file("config/checkstyle"))
        val version = SpringJavaFormatPlugin::class.java.getPackage().getImplementationVersion()
        val checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies()
        checkstyleDependencies
            .add(project.getDependencies().create("com.puppycrawl.tools:checkstyle:" + checkstyle.getToolVersion()))
        checkstyleDependencies
            .add(project.getDependencies().create("io.spring.javaformat:spring-javaformat-checkstyle:" + version))
        project.getTasks().withType<CheckFormat>(
            CheckFormat::class.java) { task: CheckFormat -> this.excludeGeneratedSources(task) }
        project.getTasks().withType<Checkstyle>(
            Checkstyle::class.java) { task: Checkstyle -> this.excludeGeneratedSources(task) }
    }

    private fun excludeGeneratedSources(task: SourceTask): SourceTask {
        return task.exclude(Spec { candidate: FileTreeElement? -> this.isGeneratedSource(candidate!!) })
    }

    private fun isGeneratedSource(candidate: FileTreeElement): Boolean {
        val path = StringUtils.cleanPath(candidate.getFile().getPath())
        return path.contains("/generated/sources/") || path.contains("/generated-source/")
    }

    private fun configureDependencyManagement(project: Project) {
        val configurations = project.getConfigurations()
        val dependencyManagement =
            configurations.create("dependencyManagement") { configuration: Configuration ->
                configuration.setVisible(false)
                configuration.setCanBeConsumed(false)
                configuration.setCanBeResolved(false)
            }
        configurations
            .matching(Spec { configuration: Configuration ->
                (configuration.name.endsWith("Classpath")
                        || JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME == configuration.name)
                        && (!configuration.name.contains("dokka"))
            })
            .all(Action { configuration: Configuration -> configuration.extendsFrom(dependencyManagement) })
        val springBootParent = project.getDependencies()
            .enforcedPlatform(
                project.getDependencies()
                    .project(
                        Collections.singletonMap<String?, String?>(
                            "path",
                            ":platform:spring-boot-internal-dependencies"
                        )
                    )
            )
        dependencyManagement.getDependencies().add(springBootParent)
        project.plugins
            .withType<OptionalDependenciesPlugin>(
                OptionalDependenciesPlugin::class.java) { optionalDependencies: OptionalDependenciesPlugin ->
                    configurations
                        .getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME)
                        .extendsFrom(dependencyManagement)
                }
    }

    private fun configureToolchain(project: Project) {
        project.plugins.apply<ToolchainPlugin>(ToolchainPlugin::class.java)
    }

    private fun configureProhibitedDependencyChecks(project: Project) {
        val sourceSets = project.getExtensions().getByType<SourceSetContainer>(SourceSetContainer::class.java)
        sourceSets.all(Action { sourceSet: SourceSet ->
            createProhibitedDependenciesChecks(
                project,
                sourceSet!!.getCompileClasspathConfigurationName(), sourceSet.getRuntimeClasspathConfigurationName()
            )
        })
    }

    private fun createProhibitedDependenciesChecks(project: Project, vararg configurationNames: String) {
        val configurations = project.getConfigurations()
        for (configurationName in configurationNames) {
            val configuration = configurations.getByName(configurationName)
            createProhibitedDependenciesCheck(configuration, project)
        }
    }

    private fun createProhibitedDependenciesCheck(classpath: Configuration, project: Project) {
        val checkClasspathForProhibitedDependencies = project
            .getTasks()
            .register<CheckClasspathForProhibitedDependencies>(
                "check" + StringUtils.capitalize(classpath.name + "ForProhibitedDependencies"),
                CheckClasspathForProhibitedDependencies::class.java) { task: CheckClasspathForProhibitedDependencies -> task!!.setClasspath(classpath) }
        project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(checkClasspathForProhibitedDependencies)
    }

    private fun configureFactoriesFilesChecks(project: Project) {
        val sourceSets: SourceSetContainer =
            project.getExtensions().getByType<JavaPluginExtension>(JavaPluginExtension::class.java).sourceSets
        sourceSets.matching(Spec { sourceSet: SourceSet? -> SourceSet.MAIN_SOURCE_SET_NAME == sourceSet!!.name })
            .configureEach(Action { main: SourceSet ->
                val check: TaskProvider<Task> = project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME)
                val checkAotFactories = project.getTasks()
                    .register<CheckAotFactories>(
                        "checkAotFactories",
                        CheckAotFactories::class.java) { task: CheckAotFactories ->
                            task!!.source = main!!.getResources()
                            task.setClasspath(main.getOutput().getClassesDirs())
                            task.setDescription("Checks the META-INF/spring/aot.factories file of the main source set.")
                        }
                check.configure(Action { task: Task -> task!!.dependsOn(checkAotFactories) })
                val checkSpringFactories = project.getTasks()
                    .register<CheckSpringFactories>(
                        "checkSpringFactories",
                        CheckSpringFactories::class.java) { task: CheckSpringFactories ->
                            task!!.source = main!!.getResources()
                            task.setClasspath(main.getOutput().getClassesDirs())
                            task.setDescription("Checks the META-INF/spring.factories file of the main source set.")
                        }
                check.configure(Action { task: Task -> task!!.dependsOn(checkSpringFactories) })
            })
    }

    private fun configureNullability(project: Project) {
        project.plugins.apply<NullabilityPlugin>(NullabilityPlugin::class.java)
        val extension =
            project.getExtensions().getByType<NullabilityPluginExtension>(NullabilityPluginExtension::class.java)
        val nullAwayVersion = project.findProperty("nullAwayVersion") as String?
        if (nullAwayVersion != null) {
            extension.getNullAwayVersion().set(nullAwayVersion)
        }
        val errorProneVersion = project.findProperty("errorProneVersion") as String?
        if (errorProneVersion != null) {
            extension.getErrorProneVersion().set(errorProneVersion)
        }
    }

    companion object {
        const val BUILD_JAVA_VERSION: Int = 25

        const val RUNTIME_JAVA_VERSION: Int = 17
    }
}
