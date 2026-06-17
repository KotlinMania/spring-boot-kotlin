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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.springframework.boot.build.properties.BuildProperties.Companion.get
import org.springframework.util.FileCopyUtils
import org.springframework.util.PropertyPlaceholderHelper
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.gradle.api.provider.Property
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.file.DirectoryProperty

/**
 * [Task] to extract resources from the classpath and write them to disk.
 * 
 * @author Andy Wilkinson
 */
abstract class ExtractResources : DefaultTask() {
    private val propertyPlaceholderHelper = PropertyPlaceholderHelper("\${", "}")

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val resourceNames: ListProperty<String>

    @get:OutputDirectory
    abstract val destinationDirectory: DirectoryProperty

    @get:Input
    abstract val properties: MapProperty<String, String>

    @TaskAction
    @Throws(IOException::class)
    fun extractResources() {
        for (resourceName in this.resourceNames.get()) {
            val resourceStream = javaClass.getClassLoader()
                .getResourceAsStream(this.packageName.getOrElse("").replace(".", "/") + "/" + resourceName)
            if (resourceStream == null) {
                throw GradleException("Resource '" + resourceName + "' does not exist")
            }
            var resource = FileCopyUtils.copyToString(InputStreamReader(resourceStream, StandardCharsets.UTF_8))
            resource = this.propertyPlaceholderHelper.replacePlaceholders(
                resource,
                PropertyPlaceholderHelper.PlaceholderResolver { key: String? -> this.properties.get().get(key) })
            FileCopyUtils.copy(
                resource,
                FileWriter(this.destinationDirectory.file(resourceName).get().asFile)
            )
        }
    }
}
