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
package org.springframework.boot.build.cli

import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.build.artifacts.ArtifactRelease.Companion.forProject
import org.springframework.boot.build.properties.BuildProperties
import org.springframework.boot.build.properties.BuildType
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty

/**
 * A [Task] for creating a Homebrew formula manifest.
 * 
 * @author Andy Wilkinson
 */
abstract class HomebrewFormula @Inject constructor(private val fileSystemOperations: FileSystemOperations) :
    DefaultTask() {
    private val buildType: BuildType?

    init {
        val project = getProject()
        val properties: MapProperty<String, Any> = this.properties
        properties.put(
            "hash",
            this.archive.map<String>(Transformer { archive: RegularFile? -> sha256(archive!!.asFile) })
        )
        this.properties.put("repo", forProject(project).downloadRepo)
        this.properties.put("version", project.version.toString())
        this.buildType = BuildProperties.get(getProject()).buildType
    }

    private fun sha256(file: File?): String? {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            return DigestUtils(digest).digestAsHex(file)
        } catch (ex: Exception) {
            throw TaskExecutionException(this, ex)
        }
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val archive: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val template: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val properties: MapProperty<String, Any>

    @TaskAction
    fun createFormula() {
        if (this.buildType != BuildType.OPEN_SOURCE) {
            Companion.logger.debug("Skipping Homebrew formula for non open source build type")
            return
        }
        this.fileSystemOperations.copy(Action { copy: CopySpec ->
            copy!!.from(this.template)
            copy.into(this.outputDir)
            copy.expand(this.properties.get())
        })
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(HomebrewFormula::class.java)
    }
}
