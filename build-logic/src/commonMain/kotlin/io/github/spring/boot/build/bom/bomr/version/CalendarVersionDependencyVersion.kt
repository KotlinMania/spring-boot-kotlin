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
import java.util.regex.Pattern

/**
 * A specialization of [ArtifactVersionDependencyVersion] for calendar versions.
 * Calendar versions are always considered to be newer than
 * [release train versions][ReleaseTrainDependencyVersion].
 * 
 * @author Andy Wilkinson
 */
class CalendarVersionDependencyVersion : ArtifactVersionDependencyVersion {
    protected constructor(artifactVersion: ArtifactVersion?) : super(artifactVersion)

    protected constructor(artifactVersion: ArtifactVersion?, comparableVersion: ComparableVersion?) : super(
        artifactVersion,
        comparableVersion
    )

    companion object {
        private val CALENDAR_VERSION_PATTERN: Pattern = Pattern.compile("\\d{4}\\.\\d+\\.\\d+(-.+)?")

        fun parse(version: String): CalendarVersionDependencyVersion? {
            if (!CALENDAR_VERSION_PATTERN.matcher(version).matches()) {
                return null
            }
            val artifactVersion: ArtifactVersion = DefaultArtifactVersion(version)
            if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier() == version) {
                return null
            }
            return CalendarVersionDependencyVersion(artifactVersion)
        }
    }
}
