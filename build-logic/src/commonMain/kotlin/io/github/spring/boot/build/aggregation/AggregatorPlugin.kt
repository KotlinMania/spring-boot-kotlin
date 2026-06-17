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
package org.springframework.boot.build.aggregation

import org.gradle.kotlin.dsl.*

import org.gradle.api.*
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyScopeConfiguration
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory

/**
 * [Plugin] for aggregating the output of other projects.
 *
 * @author Andy Wilkinson
 */
class AggregatorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val aggregates = target.objects.domainObjectContainer(Aggregate::class.java)
        target.extensions.add("aggregates", aggregates)
        aggregates.configureEach {
            val aggregate = this
            val dependencies = target.configurations.dependencyScope(aggregate.name + "Dependencies")
            val aggregated = target.configurations.resolvable(aggregate.name) {
                extendsFrom(dependencies.get())
                configureAttributes(this, aggregate, target.objects)
            }
            target.rootProject.allprojects {
                target.dependencies.add(dependencies.name, this)
            }
            aggregate.files.convention(
                aggregated.map { configuration ->
                    configuration.incoming
                        .artifactView { isLenient = true }
                        .files
                }
            )
        }
    }

    private fun configureAttributes(configuration: Configuration, aggregate: Aggregate, objects: ObjectFactory) {
        configuration.attributes {
            attributeProvider(
                Category.CATEGORY_ATTRIBUTE,
                aggregate.category.map { category -> objects.named(Category::class.java, category) }
            )
            attributeProvider(
                Usage.USAGE_ATTRIBUTE,
                aggregate.usage.map { usage -> objects.named(Usage::class.java, usage) }
            )
        }
    }
}
