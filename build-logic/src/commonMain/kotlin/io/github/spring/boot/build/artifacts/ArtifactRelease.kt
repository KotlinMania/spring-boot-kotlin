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
package org.springframework.boot.build.artifacts

import org.gradle.api.Project

/**
 * Information about artifacts produced by a build.
 * 
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class ArtifactRelease private constructor(private val type: Type) {
    fun type: String {
        return this.type.toString().lowercase()
    }

    val downloadRepo: String
        get() = if (this.type == Type.SNAPSHOT) SPRING_SNAPSHOT_REPO else MAVEN_REPO

    val isRelease: Boolean
        get() = this.type == Type.RELEASE

    enum class Type {
        SNAPSHOT, MILESTONE, RELEASE;

        companion object {
            fun forVersion(version: String): Type {
                val modifierIndex = version.lastIndexOf('-')
                if (modifierIndex == -1) {
                    return Type.RELEASE
                }
                val type = version.substring(modifierIndex + 1)
                if (type.startsWith("M") || type.startsWith("RC")) {
                    return Type.MILESTONE
                }
                return Type.SNAPSHOT
            }
        }
    }

    companion object {
        private const val SPRING_SNAPSHOT_REPO = "https://repo.spring.io/snapshot"

        private const val MAVEN_REPO = "https://repo.maven.apache.org/maven2"

        fun forProject(project: Project): ArtifactRelease {
            return forVersion(project.version.toString())
        }

        fun forVersion(version: String): ArtifactRelease {
            return ArtifactRelease(Type.Companion.forVersion(version))
        }
    }
}
