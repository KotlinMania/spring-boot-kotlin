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
package org.springframework.boot.build.properties

import org.gradle.api.Project

/**
 * Properties that can influence the build.
 * 
 * @param buildType the build type
 * @param gitHub GitHub details
 * @author Phillip Webb
 */
@JvmRecord
data class BuildProperties(val buildType: BuildType?, val gitHub: GitHub?) {
    /**
     * GitHub properties.
     * 
     * @param organization the GitHub organization
     * @param repository the GitHub repository
     */
    @JvmRecord
    data class GitHub(val organization: String?, val repository: String?) {
        companion object {
            val OPEN_SOURCE: GitHub = GitHub("spring-projects", "spring-boot")

            val COMMERCIAL: GitHub = GitHub("spring-projects", "spring-boot-commercial")
        }
    }

    companion object {
        private val PROPERTY_NAME: String = BuildProperties::class.java.getName()

        /**
         * Get the [BuildProperties] for the given [Project].
         * @param project the source project
         * @return the build properties
         */
        fun get(project: Project): BuildProperties {
            var buildProperties = project.findProperty(PROPERTY_NAME) as BuildProperties?
            if (buildProperties == null) {
                buildProperties = load(project)
                project.getExtensions().getExtraProperties().set(PROPERTY_NAME, buildProperties)
            }
            return buildProperties
        }

        private fun load(project: Project): BuildProperties {
            val buildType: BuildType = buildType(project.findProperty("spring.build-type"))
            return when (buildType) {
                BuildType.OPEN_SOURCE -> BuildProperties(buildType, GitHub.Companion.OPEN_SOURCE)
                BuildType.COMMERCIAL -> BuildProperties(buildType, GitHub.Companion.COMMERCIAL)
            }
        }

        private fun buildType(value: Any?): BuildType {
            if (value == null || "oss" == value.toString()) {
                return BuildType.OPEN_SOURCE
            }
            if ("commercial" == value.toString()) {
                return BuildType.COMMERCIAL
            }
            throw IllegalStateException("Unknown build type property '" + value + "'")
        }
    }
}
