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

import com.google.protobuf.gradle.ProtobufExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Conventions that are applied in the presence of the [ProtobufPlugin] plugin.
 * 
 * @author Eric Haag
 */
class ProtobufConventions {
    fun apply(project: Project) {
        project.plugins.withId("com.google.protobuf") { plugin: Plugin<*> ->
            val protobuf = project.getExtensions().getByType<ProtobufExtension>(ProtobufExtension::class.java)
            removeUnusedMachineSpecificConfiguration(protobuf)
        }
    }

    // See: https://github.com/google/protobuf-gradle-plugin/issues/785
    private fun removeUnusedMachineSpecificConfiguration(protobuf: ProtobufExtension) {
        protobuf.getJavaExecutablePath().convention("")
    }
}
