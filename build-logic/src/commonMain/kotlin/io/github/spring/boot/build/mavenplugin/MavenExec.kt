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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.*
import org.gradle.process.internal.ExecException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.util.function.Consumer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty

/**
 * A custom [JavaExec] [task][Task] for running Maven.
 * 
 * @author Andy Wilkinson
 */
abstract class MavenExec : JavaExec() {
    private val logger: Logger = LoggerFactory.getLogger(MavenExec::class.java)

    init {
        setClasspath(mavenConfiguration(getProject()))
        args("--batch-mode")
        getMainClass().set("org.apache.maven.cli.MavenCli")
        this.pom.set(this.projectDir.file("pom.xml"))
    }

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val pom: RegularFileProperty

    override fun exec() {
        val workingDir = this.projectDir.asFile.get()
        workingDir(workingDir)
        systemProperty("maven.multiModuleProjectDirectory", workingDir.absolutePath)
        try {
            val logFile = Files.createTempFile(getName(), ".log")
            try {
                args("--log-file", logFile.toFile().absolutePath)
                super.exec()
                if (this.logger.isInfoEnabled()) {
                    Files.readAllLines(logFile).forEach(Consumer { s: String? -> this.logger.info(s) })
                }
            } catch (ex: ExecException) {
                println("Exec exception! Dumping log")
                Files.readAllLines(logFile).forEach(Consumer { x: String? -> println(x) })
                throw ex
            }
        } catch (ex: IOException) {
            throw TaskExecutionException(this, ex)
        }
    }

    private fun mavenConfiguration(project: Project): Configuration {
        val existing = project.getConfigurations().findByName("maven")
        if (existing != null) {
            return existing
        }
        return project.getConfigurations().create("maven", Action { maven: Configuration ->
            maven.getDependencies().add(project.getDependencies().create("org.apache.maven:maven-embedder:3.6.3"))
            maven.getDependencies().add(project.getDependencies().create("org.apache.maven:maven-compat:3.6.3"))
            maven.getDependencies().add(project.getDependencies().create("org.slf4j:slf4j-simple:1.7.5"))
            maven.getDependencies()
                .add(
                    project.getDependencies()
                        .create("org.apache.maven.resolver:maven-resolver-connector-basic:1.4.1")
                )
            maven.getDependencies()
                .add(project.getDependencies().create("org.apache.maven.resolver:maven-resolver-transport-http:1.4.1"))
        })
    }
}
