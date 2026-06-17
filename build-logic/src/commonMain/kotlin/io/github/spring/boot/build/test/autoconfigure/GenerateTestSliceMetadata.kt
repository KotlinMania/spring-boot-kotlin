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
package org.springframework.boot.build.test.autoconfigure

import org.gradle.kotlin.dsl.*

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.springframework.boot.build.test.autoconfigure.TestSliceMetadata.TestSlice
import org.springframework.core.io.FileSystemResource
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory
import org.springframework.util.StringUtils
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.UncheckedIOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import javax.inject.Inject
import org.gradle.api.provider.Property
import org.gradle.api.file.RegularFileProperty

/**
 * A [Task] for generating metadata describing a project's test slices.
 * 
 * @author Andy Wilkinson
 */
abstract class GenerateTestSliceMetadata @Inject constructor(private val objectFactory: ObjectFactory) : DefaultTask() {
    private var classpath: FileCollection? = null

    private var importsFiles: FileCollection? = null

    private var classesDirs: FileCollection? = null

    init {
        this.moduleName.convention(project.name)
    }

    fun setSourceSet(sourceSet: SourceSet) {
        this.classpath = sourceSet.getRuntimeClasspath()
        this.importsFiles = this.objectFactory.fileTree()
            .from(File(sourceSet.getOutput().getResourcesDir(), "META-INF/spring"))
        this.importsFiles!!.filter(Spec { file: File? -> file!!.name.endsWith(".imports") })
        this.springFactories.set(File(sourceSet.getOutput().getResourcesDir(), "META-INF/spring.factories"))
        this.classesDirs = sourceSet.getOutput().getClassesDirs()
    }

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val springFactories: RegularFileProperty

    @get:Input
    abstract val moduleName: Property<String>

    @Classpath
    fun getClasspath(): FileCollection {
        return this.classpath!!
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val importFiles: FileCollection
        get() = this.importsFiles

    @Classpath
    fun getClassesDirs(): FileCollection {
        return this.classesDirs!!
    }

    @TaskAction
    @Throws(IOException::class)
    fun generateTestSliceMetadata() {
        val metadata = readTestSlices()
        val outputFile = this.outputFile.asFile.get()
        outputFile.parentFile.mkdirs()
        metadata.writeTo(outputFile)
    }

    @Throws(IOException::class)
    private fun readTestSlices(): TestSliceMetadata {
        val testSlices: MutableList<TestSlice?> = ArrayList<TestSlice?>()
        URLClassLoader(
            StreamSupport.stream<File?>(this.classpath!!.spliterator(), false)
                .map<URL> { file: File? -> this.toURL(file!!) }
                .toArray<URL?> { _Dummy_.__Array__() }).use { classLoader ->
            val metadataReaderFactory: MetadataReaderFactory = SimpleMetadataReaderFactory(classLoader)
            val springFactories = readSpringFactories(this.springFactories.asFile.getOrNull())
            readImportsFiles(springFactories, this.importsFiles!!)
            for (classesDir in this.classesDirs!!) {
                testSlices.addAll(readTestSlices(classesDir, metadataReaderFactory, springFactories))
            }
        }
        return TestSliceMetadata(this.moduleName.get(), testSlices)
    }

    /**
     * Reads the given imports files and puts them in springFactories. The key is the file
     * name, the value is the file contents, split by line, delimited with a comma. This
     * is done to mimic the spring.factories structure.
     * @param springFactories spring.factories parsed as properties
     * @param importsFiles the imports files to read
     */
    private fun readImportsFiles(springFactories: Properties, importsFiles: FileCollection) {
        for (file in importsFiles.files) {
            try {
                val lines = removeComments(Files.readAllLines(file.toPath()))
                val fileNameWithoutExtension = file.name
                    .substring(0, file.name.length - ".imports".length)
                springFactories.setProperty(
                    fileNameWithoutExtension,
                    StringUtils.collectionToCommaDelimitedString(lines)
                )
            } catch (ex: IOException) {
                throw UncheckedIOException("Failed to read file " + file, ex)
            }
        }
    }

    private fun removeComments(lines: MutableList<String>): MutableList<String?> {
        val result: MutableList<String?> = ArrayList<String?>()
        for (line in lines) {
            var line = line
            val commentIndex = line.indexOf('#')
            if (commentIndex > -1) {
                line = line.substring(0, commentIndex)
            }
            line = line.trim { it <= ' ' }
            if (!line.isEmpty()) {
                result.add(line)
            }
        }
        return result
    }

    private fun toURL(file: File): URL {
        try {
            return file.toURI().toURL()
        } catch (ex: MalformedURLException) {
            throw RuntimeException(ex)
        }
    }

    @Throws(IOException::class)
    private fun readSpringFactories(file: File): Properties {
        val springFactories = Properties()
        if (file.isFile()) {
            FileReader(file).use { `in` ->
                springFactories.load(`in`)
            }
        }
        return springFactories
    }

    @Throws(IOException::class)
    private fun readTestSlices(
        classesDir: File, metadataReaderFactory: MetadataReaderFactory,
        springFactories: Properties
    ): MutableList<TestSlice?> {
        Files.walk(classesDir.toPath()).use { classes ->
            return classes.filter { path: Path? -> path.toString().endsWith("Test.class") }
                .map<MetadataReader> { path: Path? -> getMetadataReader(path!!, metadataReaderFactory) }
                .filter { metadataReader: MetadataReader? -> metadataReader!!.getClassMetadata().isAnnotation() }
                .map<TestSlice> { metadataReader: MetadataReader? -> readTestSlice(metadataReader!!, springFactories) }
                .toList()
        }
    }

    private fun getMetadataReader(path: Path, metadataReaderFactory: MetadataReaderFactory): MetadataReader {
        try {
            return metadataReaderFactory.getMetadataReader(FileSystemResource(path))
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    private fun readTestSlice(metadataReader: MetadataReader, springFactories: Properties): TestSlice {
        val annotationName = metadataReader.getClassMetadata().getClassName()
        val importedAutoConfiguration = getImportedAutoConfiguration(
            springFactories,
            metadataReader.getAnnotationMetadata()
        )
        return TestSlice(annotationName, importedAutoConfiguration)
    }

    private fun getImportedAutoConfiguration(
        springFactories: Properties,
        annotationMetadata: AnnotationMetadata
    ): MutableList<String?> {
        var importers = findMetaImporters(annotationMetadata)
        if (annotationMetadata.isAnnotated("org.springframework.boot.autoconfigure.ImportAutoConfiguration")) {
            importers = Stream.concat<String?>(importers, Stream.of<String?>(annotationMetadata.getClassName()))
        }
        return importers
            .flatMap<String> { importer: String? ->
                StringUtils.commaDelimitedListToSet(
                    springFactories.getProperty(
                        importer
                    )
                ).stream()
            }
            .toList()
    }

    private fun findMetaImporters(annotationMetadata: AnnotationMetadata): Stream<String?> {
        return annotationMetadata.getAnnotationTypes()
            .stream()
            .filter { annotationType: String? -> isAutoConfigurationImporter(annotationType!!, annotationMetadata) }
    }

    private fun isAutoConfigurationImporter(annotationType: String, metadata: AnnotationMetadata): Boolean {
        return metadata.getMetaAnnotationTypes(annotationType)
            .contains("org.springframework.boot.autoconfigure.ImportAutoConfiguration")
    }
}
