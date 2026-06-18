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

import java.util.*
import java.util.function.Function

/**
 * Version of a dependency.
 * 
 * @author Andy Wilkinson
 */
interface DependencyVersion : Comparable<DependencyVersion?> {
    /**
     * Returns whether this version has the same major and minor versions as the
     * `other` version.
     * @param other the version to test
     * @return `true` if this version has the same major and minor, otherwise
     * `false`
     */
    fun isSameMinor(other: DependencyVersion?): Boolean

    /**
     * Returns whether this version has the same major version as the `other`
     * version.
     * @param other the version to test
     * @return `true` if this version has the same major, otherwise `false`
     */
    fun isSameMajor(other: DependencyVersion?): Boolean

    /**
     * Returns whether the given `candidate` is an upgrade of this version.
     * @param candidate the version to consider
     * @param movingToSnapshots whether the upgrade is to be considered as part of moving
     * to snapshots
     * @return `true` if the candidate is an upgrade, otherwise false
     */
    fun isUpgrade(candidate: DependencyVersion?, movingToSnapshots: Boolean): Boolean

    /**
     * Returns whether this version is a snapshot for the given `candidate`.
     * @param candidate the version to consider
     * @return `true` if this version is a snapshot for the candidate, otherwise
     * false
     */
    fun isSnapshotFor(candidate: DependencyVersion?): Boolean

    companion object {
        fun parse(version: String?): DependencyVersion {
            val parsers = Arrays.asList<Function<String?, DependencyVersion?>?>(
                Function { version: String? -> CalendarVersionDependencyVersion.Companion.parse(version) },
                Function { version: String? -> ArtifactVersionDependencyVersion.Companion.parse(version) },
                Function { input: String? -> ReleaseTrainDependencyVersion.Companion.parse(input) },
                Function { input: String? -> MultipleComponentsDependencyVersion.Companion.parse(input) },
                Function { version: String? -> CombinedPatchAndQualifierDependencyVersion.Companion.parse(version) },
                Function { input: String? -> LeadingZeroesDependencyVersion.Companion.parse(input) },
                Function { version: String? -> UnstructuredDependencyVersion.Companion.parse(version) })
            for (parser in parsers) {
                val result = parser.apply(version)
                if (result != null) {
                    return result
                }
            }
            throw IllegalArgumentException("Version '" + version + "' could not be parsed")
        }
    }
}
