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
package org.springframework.boot.build.antora

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.springframework.boot.build.AntoraConventions
import org.springframework.boot.build.antora.Extensions.AntoraExtensionsConfiguration
import org.springframework.boot.build.antora.Extensions.AntoraExtensionsConfiguration.RootComponent
import org.springframework.boot.build.antora.Extensions.AntoraExtensionsConfiguration.ZipContentsCollector.AlwaysInclude
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Map
import java.util.concurrent.Callable
import java.util.function.Consumer
import javax.inject.Inject
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.Property
import org.gradle.api.file.RegularFileProperty

/**
 * Task to generate a local Antora playbook.
 * 
 * @author Phillip Webb
 */
abstract class GenerateAntoraPlaybook : DefaultTask() {
    private val root: Path

    private val playbookOutputDir: Provider<String>

    private val version: String?

    @get:Nested
    val antoraExtensions: AntoraExtensions

    @get:Nested
    val asciidocExtensions: AsciidocExtensions

    @get:Nested
    val contentSource: ContentSource

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        this.root = toRealPath(getProject().getRootDir().toPath())
        this.antoraExtensions =
            getProject().getObjects().newInstance<AntoraExtensions>(AntoraExtensions::class.java, this.root)
        this.asciidocExtensions =
            getProject().getObjects().newInstance<AsciidocExtensions>(AsciidocExtensions::class.java)
        this.version = getProject().version.toString()
        this.playbookOutputDir = configurePlaybookOutputDir(getProject())
        this.contentSource = getProject().getObjects().newInstance<ContentSource>(ContentSource::class.java, this.root)
        setGroup("Documentation")
        setDescription("Generates an Antora playbook.yml file for local use")
        this.outputFile.convention(
            getProject().getLayout()
                .getBuildDirectory()
                .file("generated/docs/antora-playbook/antora-playbook.yml")
        )
        this.contentSource.addStartPath(
            getProject()
                .provider<Directory>(Callable {
                    getProject().getLayout().getProjectDirectory().dir(AntoraConventions.ANTORA_SOURCE_DIR)
                })
        )
    }

    private fun configurePlaybookOutputDir(project: Project): Provider<String> {
        val siteDirectory = getProject().getLayout().getBuildDirectory().dir("site").get().asFile.toPath()
        return project.provider<String>(Callable {
            val playbookDir: Path = toRealPath(this.outputFile.get().asFile.toPath()).getParent()
            val outputDir: Path = toRealPath(siteDirectory)
            "." + File.separator + playbookDir.relativize(outputDir)
        })
    }

    @TaskAction
    @Throws(IOException::class)
    fun writePlaybookYml() {
        val file = this.outputFile.get().asFile
        file.parentFile.mkdirs()
        FileWriter(file).use { out ->
            createYaml().dump(this.data, out)
        }
    }

    @get:Throws(IOException::class)
    private val data: MutableMap<String?, Any?>
        get() {
            val data = loadPlaybookTemplate()
            addExtensions(data)
            addSources(data)
            addDir(data)
            return data
        }

    @Throws(IOException::class)
    private fun loadPlaybookTemplate(): MutableMap<String?, Any?> {
        javaClass.getResourceAsStream("antora-playbook-template.yml").use { resource ->
            return createYaml().loadAs(resource, LinkedHashMap::class.java)
        }
    }

    private fun addExtensions(data: MutableMap<String?, Any?>) {
        val antora = data.get("antora") as MutableMap<String?, Any?>
        antora.put("extensions", Extensions.antora(Consumer { extensions: AntoraExtensionsConfiguration? ->
            extensions!!.xref(
                Consumer { xref: AntoraExtensionsConfiguration.Xref? ->
                    xref!!.stub(
                        this.antoraExtensions.xref.stubs.getOrElse(mutableListOf<String?>())
                    )
                })
            extensions.zipContentsCollector(Consumer { zipContentsCollector: AntoraExtensionsConfiguration.ZipContentsCollector? ->
                zipContentsCollector!!.versionFile("gradle.properties")
                zipContentsCollector.locations(
                    this.antoraExtensions.zipContentsCollector.locations
                        .getOrElse(mutableListOf<String?>())
                )
                zipContentsCollector
                    .alwaysInclude(this.antoraExtensions.zipContentsCollector.alwaysInclude.getOrNull())
            })
            extensions.rootComponent(Consumer { rootComponent: RootComponent? -> rootComponent!!.name("boot") })
        }))
        val asciidoc = data.get("asciidoc") as MutableMap<String?, Any?>
        var asciidocExtensions = Extensions.asciidoc()
        if (this.asciidocExtensions.excludeJavadocExtension.getOrElse(java.lang.Boolean.FALSE)) {
            asciidocExtensions = ArrayList<String?>(asciidocExtensions)
            asciidocExtensions.remove("@springio/asciidoctor-extensions/javadoc-extension")
        }
        asciidoc.put("extensions", asciidocExtensions)
    }

    private fun addSources(data: MutableMap<String?, Any?>) {
        val contentSources = getList<MutableMap<String?, Any?>?>(data, "content.sources")
        contentSources.add(createContentSource())
    }

    private fun createContentSource(): MutableMap<String?, Any?> {
        val source: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        val playbookPath = this.outputFile.get().asFile.toPath().getParent()
        val url = StringBuilder(".")
        this.root.relativize(playbookPath).normalize()
            .forEach(Consumer { path: Path? -> url.append(File.separator).append("..") })
        source.put("url", url.toString())
        source.put("branches", "HEAD")
        source.put("version", this.version)
        source.put("start_paths", this.contentSource.startPaths.get())
        return source
    }

    private fun addDir(data: MutableMap<String?, Any?>) {
        data.put("output", Map.of<String?, String?>("dir", this.playbookOutputDir.get()))
    }

    private fun <T> getList(data: MutableMap<String?, Any?>, location: String): MutableList {
        return get(data, location) as MutableList
    }

    private fun get(data: MutableMap<String?, Any?>, location: String): Any {
        var result: Any = data
        val keys: Array<String?> = location.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (key in keys) {
            result = (result as MutableMap<String?, Any>).get(key)!!
        }
        return result
    }

    private fun createYaml(): Yaml {
        val options = DumperOptions()
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        options.setPrettyFlow(true)
        return Yaml(options)
    }

    abstract class AntoraExtensions @Inject constructor(objects: ObjectFactory, root: Path) {
        @get:Nested
        val xref: Xref

        @get:Nested
        val zipContentsCollector: ZipContentsCollector

        init {
            this.xref = objects.newInstance<Xref>(Xref::class.java)
            this.zipContentsCollector =
                objects.newInstance<ZipContentsCollector>(ZipContentsCollector::class.java, root)
        }

        abstract class Xref {
            @get:Optional
            @get:Input
            abstract val stubs: ListProperty<String>
        }

        abstract class ZipContentsCollector @Inject constructor(project: Project, root: Path) {
            @get:Optional
            @get:Input
            val locations: Provider<MutableList<String?>>

            init {
                this.locations = configureZipContentCollectorLocations(project, root)
            }

            private fun configureZipContentCollectorLocations(
                project: Project,
                root: Path
            ): Provider<MutableList<String?>> {
                val locations = project.getObjects().listProperty<String>(String::class.java)
                val relativeProjectPath: Path = relativize(root, project.projectDir.toPath())
                val locationName = project.name + "-\${version}-\${name}-\${classifier}.zip"
                locations.add(
                    project
                        .provider<String>(Callable {
                            relativeProjectPath.resolve(GENERATED_DOCS + "antora-content/" + locationName)
                                .toString()
                        })
                )
                locations.addAll(this.dependencies.map<MutableList<String?>?>(Transformer { dependencies: MutableSet<String?>? ->
                    dependencies!!.stream()
                        .map<Path> { dependency: String? ->
                            relativeProjectPath
                                .resolve(GENERATED_DOCS + "antora-dependencies-content/" + dependency + "/" + locationName)
                        }
                        .map<String> { obj: Path? -> obj.toString() }
                        .toList()
                }))
                return locations
            }

            @get:Optional
            @get:Input
            abstract val alwaysInclude: ListProperty<AlwaysInclude>

            @get:Optional
            @get:Input
            abstract val dependencies: SetProperty<String>

            companion object {
                private fun relativize(root: Path, subPath: Path): Path {
                    return toRealPath(root).relativize(toRealPath(subPath)).normalize()
                }
            }
        }
    }

    abstract class AsciidocExtensions @Inject constructor() {
        @get:Optional
        @get:Input
        abstract val excludeJavadocExtension: Property<Boolean>
    }

    abstract class ContentSource @Inject constructor(private val root: Path) {
        @get:Input
        abstract val startPaths: ListProperty<String>

        fun addStartPath(startPath: Provider<Directory>) {
            this.startPaths
                .add(startPath.map<String>(Transformer { dir: Directory? ->
                    this.root.relativize(
                        toRealPath(
                            dir!!.asFile.toPath()
                        )
                    ).toString()
                }))
        }
    }

    companion object {
        private const val GENERATED_DOCS = "build/generated/docs/"

        private fun toRealPath(path: Path): Path {
            try {
                return if (Files.exists(path)) path.toRealPath() else path
            } catch (ex: IOException) {
                throw UncheckedIOException(ex)
            }
        }
    }
}
