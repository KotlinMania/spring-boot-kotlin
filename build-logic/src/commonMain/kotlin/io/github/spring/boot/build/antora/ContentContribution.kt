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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip

/**
 * A contribution of content to Antora.
 * 
 * @author Andy Wilkinson
 */
abstract class ContentContribution protected constructor(
    project: Project?,
    name: String?,
    protected val type: String
) : Contribution(project, name) {
    abstract fun produceFrom(copySpec: CopySpec?)

    protected fun configureProduction(copySpec: CopySpec): TaskProvider<out Task?> {
        val tasks = getProject().getTasks()
        val zipContent = tasks.register<Zip>(
            taskName("zip", "%sAntora%sContent", getName(), this.type),
            Zip::class.java, Action { zip: Zip ->
                zip!!.destinationDirectory
                    .set(getProject().getLayout().getBuildDirectory().dir("generated/docs/antora-content"))
                zip.getArchiveClassifier().set("%s-%s-content".format(getName(), this.type))
                zip.with(copySpec)
                zip.setDescription(
                    "Creates a zip archive of the %s Antora %s content.".format(
                        getName(),
                        toDescription(this.type)
                    )
                )
            })
        configureAntora(addInputFrom(zipContent, zipContent.name))
        return zipContent
    }

    companion object {
        private fun toDescription(input: String): String {
            return input.replace("-", " ")
        }
    }
}
