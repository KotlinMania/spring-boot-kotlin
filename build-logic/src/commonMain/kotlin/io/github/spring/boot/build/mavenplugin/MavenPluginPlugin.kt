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
package org.springframework.boot.build.mavenplugin

import io.spring.javaformat.formatter.FileEdit
import io.spring.javaformat.formatter.FileFormatter
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ConfigurationVariantDetails
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.springframework.boot.build.DeployedPlugin
import org.springframework.boot.build.MavenRepositoryPlugin
import org.springframework.boot.build.bom.ResolvedBom
import org.springframework.boot.build.bom.ResolvedBom.Companion.readFrom
import org.springframework.boot.build.optional.OptionalDependenciesPlugin
import org.springframework.boot.build.test.DockerTestPlugin
import org.springframework.boot.build.test.IntegrationTestPlugin
import org.springframework.core.CollectionFactory
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import javax.inject.Inject

/**
 * Plugin for building Spring Boot's Maven Plugin.
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class MavenPluginPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply<JavaLibraryPlugin>(JavaLibraryPlugin::class.java)
        project.plugins.apply<MavenPublishPlugin>(MavenPublishPlugin::class.java)
        project.plugins.apply<DeployedPlugin>(DeployedPlugin::class.java)
        project.plugins.apply<MavenRepositoryPlugin>(MavenRepositoryPlugin::class.java)
        project.plugins.apply<IntegrationTestPlugin>(IntegrationTestPlugin::class.java)
        val jarTask = project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME) as Jar
        configurePomPackaging(project)
        addPopulateIntTestMavenRepositoryTask(project)
        val generateHelpMojoTask: TaskProvider<MavenExec> = addGenerateHelpMojoTask(project, jarTask)
        val generatePluginDescriptorTask: TaskProvider<MavenExec> = addGeneratePluginDescriptorTask(
            project, jarTask,
            generateHelpMojoTask
        )
        addDocumentPluginGoalsTask(project, generatePluginDescriptorTask)
        addPrepareMavenBinariesTask(project)
        val extractVersionPropertiesTask: TaskProvider<ExtractVersionProperties> =
            addExtractVersionPropertiesTask(project)
        project.getTasks()
            .named(IntegrationTestPlugin.INT_TEST_TASK_NAME)
            .configure(Action { task: Task ->
                task!!.getInputs()
                    .file(extractVersionPropertiesTask.map<RegularFileProperty>(Transformer { obj: ExtractVersionProperties? -> obj!!.destination }))
                    .withPathSensitivity(PathSensitivity.RELATIVE)
                    .withPropertyName("versionProperties")
            })
        publishOptionalDependenciesInPom(project)
        project.getTasks().withType<GenerateModuleMetadata>(GenerateModuleMetadata::class.java)
            .configureEach(Action { task: GenerateModuleMetadata -> task!!.setEnabled(false) })
    }

    private fun publishOptionalDependenciesInPom(project: Project) {
        project.plugins.withType<OptionalDependenciesPlugin>(
            OptionalDependenciesPlugin::class.java) { optionalDependencies: OptionalDependenciesPlugin ->
                val component = project.getComponents().findByName("java")
                if (component is AdhocComponentWithVariants) {
                    component.addVariantsFromConfiguration(
                        project.getConfigurations().getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME)) { obj: ConfigurationVariantDetails -> obj!!.mapToOptional() }
                }
            }
        val publication = project.getExtensions()
            .getByType<PublishingExtension>(PublishingExtension::class.java)
            .publications
            .getByName("maven") as MavenPublication
        publication.getPom().withXml(Action { xml: XmlProvider ->
            val root = xml!!.asElement()
            val children = root.getChildNodes()
            for (i in 0..<children.getLength()) {
                val child = children.item(i)
                if ("dependencyManagement" == child.getNodeName()) {
                    root.removeChild(child)
                }
            }
        })
    }

    private fun configurePomPackaging(project: Project) {
        val publishing = project.getExtensions().getByType<PublishingExtension>(PublishingExtension::class.java)
        publishing.publications.withType<MavenPublication>(
            MavenPublication::class.java) { mavenPublication: MavenPublication -> this.setPackaging(mavenPublication) }
    }

    private fun setPackaging(mavenPublication: MavenPublication) {
        mavenPublication.pom(Action { pom: MavenPom -> pom!!.setPackaging("maven-plugin") })
    }

    private fun addPopulateIntTestMavenRepositoryTask(project: Project) {
        val repositoryContents = project.getConfigurations().create("repositoryContents")
        repositoryContents.extendsFrom(
            project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME),
            project.getConfigurations().getByName("mavenRepository")
        )
        repositoryContents.attributes(Action { attributes: AttributeContainer ->
            attributes!!.attribute<DocsType>(
                DocsType.DOCS_TYPE_ATTRIBUTE,
                project.getObjects().named<DocsType>(DocsType::class.java, "maven-repository")
            )
        })
        repositoryContents.setCanBeConsumed(false)
        val populateMavenRepository = project.getTasks()
            .register<ResolvedConfigurationMavenRepository>(
                "populateResolvedDependenciesMavenRepository", ResolvedConfigurationMavenRepository::class.java) { task: ResolvedConfigurationMavenRepository ->
                    task!!.setConfiguration(repositoryContents)
                    task.outputDir
                        .set(project.getLayout().getBuildDirectory().dir("resolved-dependencies-maven-repository"))
                }
        project.getDependencies()
            .components(Action { components: ComponentMetadataHandler ->
                components!!.all(
                    MavenRepositoryComponentMetadataRule::class.java
                )
            })
        val populateRepository = project.getTasks()
            .register<Sync>("populateTestMavenRepository", Sync::class.java) { task: Sync ->
                task!!.setDestinationDir(
                    project.getLayout().getBuildDirectory().dir("test-maven-repository").get().asFile
                )
                task.with(copyIntTestMavenRepositoryFiles(project, populateMavenRepository))
                task.dependsOn(
                    project.getTasks().getByName(MavenRepositoryPlugin.PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME)
                )
            }
        project.getTasks().getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME).dependsOn(populateRepository)
        project.plugins
            .withType<DockerTestPlugin>(DockerTestPlugin::class.java)
            .all(Action { dockerTestPlugin: DockerTestPlugin ->
                project.getTasks()
                    .named(
                        DockerTestPlugin.DOCKER_TEST_TASK_NAME) { dockerTest: Task -> dockerTest!!.dependsOn(populateRepository) }
            })
    }

    private fun copyIntTestMavenRepositoryFiles(
        project: Project,
        runtimeClasspathMavenRepository: TaskProvider<ResolvedConfigurationMavenRepository>
    ): CopySpec {
        val copySpec = project.copySpec()
        copySpec.from(project.getConfigurations().getByName(MavenRepositoryPlugin.MAVEN_REPOSITORY_CONFIGURATION_NAME))
        copySpec.from(project.getLayout().getBuildDirectory().dir("maven-repository"))
        copySpec.from(runtimeClasspathMavenRepository)
        return copySpec
    }

    private fun addDocumentPluginGoalsTask(project: Project, generatePluginDescriptorTask: TaskProvider<MavenExec>) {
        project.getTasks().register<DocumentPluginGoals>(
            "documentPluginGoals",
            DocumentPluginGoals::class.java) { task: DocumentPluginGoals ->
                val layout = project.getLayout()
                val pluginXml: Provider<RegularFile> = layout.file(
                    generatePluginDescriptorTask
                        .map<File>(Transformer { generateDescriptor: MavenExec? ->
                            java.io.File(
                                generateDescriptor!!.getOutputs().files.singleFile,
                                "plugin.xml"
                            )
                        })
                )
                task!!.pluginXml.set(pluginXml)
                task.outputDir.set(layout.getBuildDirectory().dir("docs/generated/goals/"))
                task.dependsOn(generatePluginDescriptorTask)
            }
    }

    private fun addGenerateHelpMojoTask(project: Project, jarTask: Jar): TaskProvider<MavenExec> {
        val helpMojoDir: Provider<Directory> = project.getLayout().getBuildDirectory().dir("help-mojo")
        val syncHelpMojoInputs: TaskProvider<Sync> = createSyncHelpMojoInputsTask(project, helpMojoDir)
        val task: TaskProvider<MavenExec> = createGenerateHelpMojoTask(project, helpMojoDir, syncHelpMojoInputs)
        includeHelpMojoInJar(jarTask, task)
        return task
    }

    private fun createGenerateHelpMojoTask(
        project: Project, helpMojoDir: Provider<Directory>,
        syncHelpMojoInputs: TaskProvider<Sync>
    ): TaskProvider<MavenExec> {
        return project.getTasks()
            .register<MavenExec>("generateHelpMojo", MavenExec::class.java) { task: MavenExec ->
                task!!.projectDir.set(helpMojoDir)
                task.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.1:helpmojo")
                task.getOutputs()
                    .dir(helpMojoDir.map<Directory>(Transformer { directory: Directory? -> directory!!.dir("target/generated-sources/plugin") }))
                task.dependsOn(syncHelpMojoInputs)
            }
    }

    private fun createSyncHelpMojoInputsTask(project: Project, helpMojoDir: Provider<Directory>): TaskProvider<Sync> {
        return project.getTasks().register<Sync>("syncHelpMojoInputs", Sync::class.java) { task: Sync ->
            task!!.setDestinationDir(helpMojoDir.get()!!.asFile)
            val pomFile: File = java.io.File(project.projectDir, "src/maven/resources/pom.xml")
            task.from(pomFile) { copy: CopySpec -> replaceVersionPlaceholder(copy!!, project) }
        }
    }

    private fun includeHelpMojoInJar(jarTask: Jar, generateHelpMojoTask: TaskProvider<MavenExec>) {
        jarTask.from(generateHelpMojoTask).exclude("**/*.java")
        jarTask.dependsOn(generateHelpMojoTask)
    }

    private fun addGeneratePluginDescriptorTask(
        project: Project, jarTask: Jar,
        generateHelpMojoTask: TaskProvider<MavenExec>
    ): TaskProvider<MavenExec> {
        val pluginDescriptorDir: Provider<Directory> = project.getLayout().getBuildDirectory().dir("plugin-descriptor")
        val generatedHelpMojoDir: Provider<Directory> = project.getLayout()
            .getBuildDirectory()
            .dir("generated/sources/helpMojo")
        val mainSourceSet = getMainSourceSet(project)
        project.getTasks()
            .withType<Javadoc>(Javadoc::class.java) { javadoc: Javadoc -> this.setJavadocOptions(javadoc) }
        val formattedHelpMojoSource: TaskProvider<FormatHelpMojoSource> = createFormatHelpMojoSource(
            project,
            generateHelpMojoTask, generatedHelpMojoDir
        )
        project.getTasks().getByName(mainSourceSet.getCompileJavaTaskName()).dependsOn(formattedHelpMojoSource)
        mainSourceSet.java(Action { javaSources: SourceDirectorySet -> javaSources!!.srcDir(formattedHelpMojoSource) })
        val pluginDescriptorInputs: TaskProvider<Sync> = createSyncPluginDescriptorInputs(
            project, pluginDescriptorDir,
            mainSourceSet
        )
        val task: TaskProvider<MavenExec> = createGeneratePluginDescriptorTask(
            project, pluginDescriptorDir,
            pluginDescriptorInputs
        )
        includeDescriptorInJar(jarTask, task)
        return task
    }

    private fun getMainSourceSet(project: Project): SourceSet {
        val sourceSets: SourceSetContainer =
            project.getExtensions().getByType<JavaPluginExtension>(JavaPluginExtension::class.java).sourceSets
        return sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
    }

    private fun setJavadocOptions(javadoc: Javadoc) {
        val options = javadoc.getOptions() as StandardJavadocDocletOptions
        options.addMultilineStringsOption("tag")
            .setValue(mutableListOf<String?>("goal:X", "requiresProject:X", "threadSafe:X"))
    }

    private fun createFormatHelpMojoSource(
        project: Project,
        generateHelpMojoTask: TaskProvider<MavenExec>, generatedHelpMojoDir: Provider<Directory>
    ): TaskProvider<FormatHelpMojoSource> {
        return project.getTasks().register<FormatHelpMojoSource>(
            "formatHelpMojoSource",
            FormatHelpMojoSource::class.java) { task: FormatHelpMojoSource ->
                task!!.setGenerator(generateHelpMojoTask)
                task.outputDir.set(generatedHelpMojoDir)
            }
    }

    private fun createSyncPluginDescriptorInputs(
        project: Project, destination: Provider<Directory>,
        sourceSet: SourceSet
    ): TaskProvider<Sync> {
        return project.getTasks()
            .register<Sync>("syncPluginDescriptorInputs", Sync::class.java) { task: Sync ->
                task!!.setDestinationDir(destination.get()!!.asFile)
                val pomFile: File = java.io.File(project.projectDir, "src/maven/resources/pom.xml")
                task.from(pomFile) { copy: CopySpec -> replaceVersionPlaceholder(copy!!, project) }
                task.from(
                    sourceSet.getOutput().getClassesDirs()) { sync: CopySpec -> sync!!.into("target/classes") }
                task.from(
                    sourceSet.getAllJava().getSrcDirs()) { sync: CopySpec -> sync!!.into("src/main/java") }
                task.getInputs().property("version", project.version)
                task.dependsOn(sourceSet.getClassesTaskName())
            }
    }

    private fun createGeneratePluginDescriptorTask(
        project: Project, mavenDir: Provider<Directory>,
        pluginDescriptorInputs: TaskProvider<Sync>
    ): TaskProvider<MavenExec> {
        return project.getTasks()
            .register<MavenExec>("generatePluginDescriptor", MavenExec::class.java) { task: MavenExec ->
                task!!.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.1:descriptor")
                task.getOutputs()
                    .dir(mavenDir.map<Directory>(Transformer { directory: Directory? -> directory!!.dir("target/classes/META-INF/maven") }))
                task.getInputs()
                    .dir(mavenDir.map<Directory>(Transformer { directory: Directory? -> directory!!.dir("target/classes/org") }))
                    .withPathSensitivity(PathSensitivity.RELATIVE)
                    .withPropertyName("plugin classes")
                task.projectDir.set(mavenDir)
                task.dependsOn(pluginDescriptorInputs)
            }
    }

    private fun includeDescriptorInJar(jar: Jar, generatePluginDescriptorTask: TaskProvider<MavenExec>) {
        jar.from(generatePluginDescriptorTask) { copy: CopySpec -> copy!!.into("META-INF/maven/") }
        jar.dependsOn(generatePluginDescriptorTask)
    }

    private fun addPrepareMavenBinariesTask(project: Project) {
        val task = project.getTasks()
            .register<PrepareMavenBinaries>(
                "prepareMavenBinaries", PrepareMavenBinaries::class.java) { prepareMavenBinaries: PrepareMavenBinaries ->
                    prepareMavenBinaries!!.outputDir
                        .set(project.getLayout().getBuildDirectory().dir("maven-binaries"))
                }
        project.getTasks()
            .getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME)
            .getInputs()
            .dir(task.map<DirectoryProperty>(Transformer { obj: PrepareMavenBinaries? -> obj!!.outputDir }))
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .withPropertyName("mavenBinaries")
    }

    private fun replaceVersionPlaceholder(copy: CopySpec, project: Project) {
        copy.filter(Transformer { input: String? -> replaceVersionPlaceholder(project, input!!) })
    }

    private fun replaceVersionPlaceholder(project: Project, input: String): String {
        return input.replace("{{version}}", project.version.toString())
    }

    private fun addExtractVersionPropertiesTask(project: Project): TaskProvider<ExtractVersionProperties> {
        return project.getTasks().register<ExtractVersionProperties>(
            "extractVersionProperties",
            ExtractVersionProperties::class.java) { task: ExtractVersionProperties ->
                task!!.setResolvedBoms(project.getConfigurations().create("versionProperties"))
                task.destination
                    .set(
                        project.getLayout()
                            .getBuildDirectory()
                            .dir("generated-resources")
                            .map<RegularFile>(Transformer { dir: Directory? -> dir!!.file("extracted-versions.properties") })
                    )
            }
    }

    abstract class FormatHelpMojoSource @Inject constructor(private val objectFactory: ObjectFactory) : DefaultTask() {
        private var generator: TaskProvider<*> = null

        fun setGenerator(generator: TaskProvider<*>) {
            this.generator = generator
            getInputs().files(this.generator)
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .withPropertyName("generated source")
        }

        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        @TaskAction
        fun syncAndFormat() {
            val formatter = FileFormatter()
            for (output in this.generator!!.get().getOutputs().files) {
                formatter.formatFiles(this.objectFactory.fileTree().from(output), StandardCharsets.UTF_8)
                    .forEach { edit: FileEdit? -> save(output, edit!!) }
            }
        }

        private fun save(output: File, edit: FileEdit) {
            val relativePath = output.toPath().relativize(edit.getFile().toPath())
            val outputLocation = this.outputDir.asFile.get().toPath().resolve(relativePath)
            try {
                Files.createDirectories(outputLocation.getParent())
                var content = edit.getFormattedContent()
                content = addNullAwaySuppression(content)
                Files.writeString(outputLocation, content)
            } catch (ex: Exception) {
                throw TaskExecutionException(this, ex)
            }
        }

        private fun addNullAwaySuppression(content: String): String {
            val separator = System.lineSeparator()
            val lines = content.split(separator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val result = StringBuilder()
            for (line in lines) {
                if (line.startsWith("public class ")) {
                    result.append("@SuppressWarnings(\"NullAway\")").append(separator)
                }
                result.append(line).append(separator)
            }
            return result.toString()
        }
    }

    class MavenRepositoryComponentMetadataRule @Inject constructor(private val objects: ObjectFactory) :
        ComponentMetadataRule {
        override fun execute(context: ComponentMetadataContext) {
            context.getDetails()
                .maybeAddVariant(
                    "compileWithMetadata",
                    "compile") { variant: VariantMetadata -> configureVariant(context, variant!!) }
            context.getDetails()
                .maybeAddVariant(
                    "apiElementsWithMetadata", "apiElements") { variant: VariantMetadata -> configureVariant(context, variant!!) }
        }

        private fun configureVariant(context: ComponentMetadataContext, variant: VariantMetadata) {
            variant.attributes(Action { attributes: AttributeContainer ->
                attributes!!.attribute<DocsType>(
                    DocsType.DOCS_TYPE_ATTRIBUTE,
                    this.objects.named<DocsType>(DocsType::class.java, "maven-repository")
                )
                attributes.attribute<Usage>(
                    Usage.USAGE_ATTRIBUTE,
                    this.objects.named<Usage>(Usage::class.java, "maven-repository")
                )
            })
            variant.withFiles(Action { files: MutableVariantFilesMetadata ->
                val id = context.getDetails().id
                files!!.addFile(id.name + "-" + id.version + ".pom")
            })
        }
    }

    abstract class ResolvedConfigurationMavenRepository : DefaultTask() {
        private var configuration: Configuration? = null

        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        @Classpath
        fun getConfiguration(): Configuration {
            return this.configuration!!
        }

        fun setConfiguration(configuration: Configuration) {
            this.configuration = configuration
        }

        @TaskAction
        fun createRepository() {
            for (result in this.configuration!!.getIncoming().getArtifacts()) {
                if (result.id.getComponentIdentifier() is ModuleComponentIdentifier) {
                    val fileName: String = result.getFile()
                        .name
                        .replace(identifier.version + "-" + identifier.version, identifier.version)
                    val repositoryLocation = this.outputDir
                        .dir(
                            (identifier.getGroup().replace('.', '/') + "/" + identifier.getModule() + "/"
                                    + identifier.version + "/" + fileName)
                        )
                        .get()
                        .asFile
                    repositoryLocation.parentFile.mkdirs()
                    try {
                        Files.copy(
                            result.getFile().toPath(), repositoryLocation.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    } catch (ex: IOException) {
                        throw RuntimeException("Failed to copy artifact '" + result + "'", ex)
                    }
                }
            }
        }
    }

    abstract class ExtractVersionProperties : DefaultTask() {
        private var resolvedBoms: FileCollection = null

        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        fun getResolvedBoms(): FileCollection {
            return this.resolvedBoms!!
        }

        fun setResolvedBoms(resolvedBoms: FileCollection) {
            this.resolvedBoms = resolvedBoms
        }

        @get:OutputFile
        abstract val destination: RegularFileProperty

        @TaskAction
        fun extractVersionProperties() {
            val resolvedBom = readFrom(this.resolvedBoms!!.singleFile)
            val versions = extractVersionProperties(resolvedBom!!)
            writeProperties(versions)
        }

        private fun writeProperties(versions: Properties) {
            val outputFile = this.destination.asFile.get()
            outputFile.parentFile.mkdirs()
            try {
                FileWriter(outputFile).use { writer ->
                    versions.store(writer, null)
                }
            } catch (ex: IOException) {
                throw GradleException("Failed to write extracted version properties", ex)
            }
        }

        private fun extractVersionProperties(resolvedBom: ResolvedBom): Properties {
            val versions = CollectionFactory.createSortedProperties(true)
            versions.setProperty("project.version", resolvedBom.id!!.version)
            val versionProperties = mutableSetOf<String?>(
                "log4j2.version", "maven-jar-plugin.version",
                "maven-war-plugin.version", "build-helper-maven-plugin.version", "spring-framework.version",
                "jakarta-servlet.version", "kotlin.version", "assertj.version", "junit-jupiter.version"
            )
            for (library in resolvedBom.libraries!!) {
                if (library!!.versionProperty != null && versionProperties.contains(library.versionProperty)) {
                    versions.setProperty(library.versionProperty, library.version)
                }
            }
            return versions
        }
    }
}
