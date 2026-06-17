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

import org.antora.gradle.AntoraPlugin
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import javax.inject.Inject

/**
 * [Plugin] for a project that contributes to Antora-based documentation that is
 * [depended upon][AntoraDependenciesPlugin] by another project.
 * 
 * @author Andy Wilkinson
 */
class AntoraContributorPlugin : Plugin<Project?> {
    override fun apply(project: Project) {
        project.getPlugins().apply<AntoraPlugin?>(AntoraPlugin::class.java)
        val antoraContributions = project.getObjects()
            .domainObjectContainer<Contribution?>(
                Contribution::class.java,
                NamedDomainObjectFactory { name: String? ->
                    project.getObjects().newInstance<Contribution?>(
                        org.springframework.boot.build.antora.AntoraContributorPlugin.Contribution::class.java,
                        name,
                        project
                    )!!
                })
        project.getExtensions().add("antoraContributions", antoraContributions)
    }

    class Contribution @Inject constructor(val name: String?, private val project: Project) {
        private var publish = false

        fun publish() {
            this.publish = true
        }

        fun source() {
            SourceContribution(this.project, this.name).produce()
        }

        fun catalogContent(action: Action<CopySpec?>) {
            val copySpec = this.project.copySpec()
            action.execute(copySpec)
            CatalogContentContribution(this.project, this.name).produceFrom(copySpec, this.publish)
        }

        fun aggregateContent(action: Action<CopySpec?>) {
            val copySpec = this.project.copySpec()
            action.execute(copySpec)
            AggregateContentContribution(this.project, this.name).produceFrom(copySpec, this.publish)
        }

        fun localAggregateContent(action: Action<CopySpec?>) {
            val copySpec = this.project.copySpec()
            action.execute(copySpec)
            LocalAggregateContentContribution(this.project, this.name).produceFrom(copySpec)
        }
    }
}
