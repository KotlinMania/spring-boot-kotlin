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
package org.springframework.boot.build.bom.bomr.version

import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.ComparableVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

/**
 * A fallback [DependencyVersion] to handle versions with four or five components
 * that cannot be handled by [ArtifactVersion] because the fourth component is
 * numeric.
 * 
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
internal class MultipleComponentsDependencyVersion private constructor(
    artifactVersion: ArtifactVersion?,
    private val original: String
) : ArtifactVersionDependencyVersion(
    artifactVersion, ComparableVersion(
        original
    )
) {
    public override fun toString(): String {
        return this.original
    }

    companion object {
        fun parse(input: String): MultipleComponentsDependencyVersion? {
            val components: Array<String?> = input.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (components.size == 4 || components.size == 5) {
                val artifactVersion: ArtifactVersion = DefaultArtifactVersion(
                    components[0] + "." + components[1] + "." + components[2]
                )
                if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier() == input) {
                    return null
                }
                return MultipleComponentsDependencyVersion(artifactVersion, input)
            }
            return null
        }
    }
}
