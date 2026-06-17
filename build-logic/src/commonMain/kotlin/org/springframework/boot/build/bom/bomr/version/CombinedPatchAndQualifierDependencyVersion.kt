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
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A [DependencyVersion] where the patch and qualifier are not separated.
 * 
 * @author Andy Wilkinson
 */
internal class CombinedPatchAndQualifierDependencyVersion private constructor(
    artifactVersion: ArtifactVersion?,
    private val original: String?
) : ArtifactVersionDependencyVersion(artifactVersion) {
    public override fun toString(): String {
        return this.original!!
    }

    companion object {
        private val PATTERN: Pattern = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+)([A-Za-z][A-Za-z0-9]+)")

        fun parse(version: String): CombinedPatchAndQualifierDependencyVersion? {
            val matcher: Matcher = PATTERN.matcher(version)
            if (!matcher.matches()) {
                return null
            }
            val artifactVersion: ArtifactVersion = DefaultArtifactVersion(matcher.group(1) + "." + matcher.group(2))
            if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier() == version) {
                return null
            }
            return CombinedPatchAndQualifierDependencyVersion(artifactVersion, version)
        }
    }
}
