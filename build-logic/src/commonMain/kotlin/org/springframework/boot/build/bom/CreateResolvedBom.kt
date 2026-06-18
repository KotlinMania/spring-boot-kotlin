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
package org.springframework.boot.build.bom

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject
import org.gradle.api.file.RegularFileProperty

/**
 * [Task] to create a [resolved bom][ResolvedBom].
 * 
 * @author Andy Wilkinson
 */
abstract class CreateResolvedBom @Inject constructor(bomExtension: BomExtension) : DefaultTask() {
    private val bomExtension: BomExtension

    private val bomResolver: BomResolver

    init {
        getOutputs().upToDateWhen(Spec { spec: Task? -> false })
        this.bomExtension = bomExtension
        this.bomResolver = BomResolver(project.getConfigurations(), project.getDependencies())
        this.outputFile.convention(project.getLayout().getBuildDirectory().file(name + "/resolved-bom.json"))
    }

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    @Throws(IOException::class)
    fun createResolvedBom() {
        val resolvedBom = this.bomResolver.resolve(this.bomExtension)
        FileWriter(this.outputFile.get().asFile).use { writer ->
            resolvedBom.writeTo(writer)
        }
    }
}
