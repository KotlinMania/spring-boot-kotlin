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

import org.springframework.util.StringUtils
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A [DependencyVersion] for a release train such as Spring Data.
 * 
 * @author Andy Wilkinson
 */
internal class ReleaseTrainDependencyVersion private constructor(
    private val releaseTrain: String,
    private val type: String,
    private val version: Int,
    private val original: String
) : DependencyVersion {
    override fun compareTo(other: DependencyVersion?): Int {
        if (other !is ReleaseTrainDependencyVersion) {
            return -1
        }
        var comparison = this.releaseTrain.compareTo(other.releaseTrain)
        if (comparison != 0) {
            return comparison
        }
        comparison = this.type.compareTo(other.type)
        if (comparison != 0) {
            return comparison
        }
        return Integer.compare(this.version, other.version)
    }

    override fun isUpgrade(candidate: DependencyVersion?, movingToSnapshots: Boolean): Boolean {
        if (candidate is ReleaseTrainDependencyVersion) {
            return isUpgrade(candidate, movingToSnapshots)
        }
        return true
    }

    private fun isUpgrade(candidate: ReleaseTrainDependencyVersion, movingToSnapshots: Boolean): Boolean {
        var comparison = this.releaseTrain.compareTo(candidate.releaseTrain)
        if (comparison != 0) {
            return comparison < 0
        }
        if (movingToSnapshots && !this.isSnapshot && candidate.isSnapshot) {
            return true
        }
        comparison = this.type.compareTo(candidate.type)
        if (comparison != 0) {
            return comparison < 0
        }
        return Integer.compare(this.version, candidate.version) < 0
    }

    private val isSnapshot: Boolean
        get() = "BUILD-SNAPSHOT" == this.type

    override fun isSnapshotFor(candidate: DependencyVersion?): Boolean {
        if (!this.isSnapshot || candidate !is ReleaseTrainDependencyVersion) {
            return false
        }
        return this.releaseTrain == candidate.releaseTrain
    }

    override fun isSameMajor(other: DependencyVersion?): Boolean {
        return isSameReleaseTrain(other)
    }

    override fun isSameMinor(other: DependencyVersion?): Boolean {
        return isSameReleaseTrain(other)
    }

    private fun isSameReleaseTrain(other: DependencyVersion?): Boolean {
        if (other is CalendarVersionDependencyVersion) {
            return false
        }
        if (other is ReleaseTrainDependencyVersion) {
            return other.releaseTrain == this.releaseTrain
        }
        return true
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as ReleaseTrainDependencyVersion
        return this.original == other.original
    }

    override fun hashCode(): Int {
        return this.original.hashCode()
    }

    override fun toString(): String {
        return this.original
    }

    companion object {
        private val VERSION_PATTERN: Pattern = Pattern
            .compile("([A-Z][a-z]+)-((BUILD-SNAPSHOT)|([A-Z-]+)([0-9]*))")

        fun parse(input: String): ReleaseTrainDependencyVersion? {
            val matcher: Matcher = VERSION_PATTERN.matcher(input)
            if (!matcher.matches()) {
                return null
            }
            return ReleaseTrainDependencyVersion(
                matcher.group(1),
                if (StringUtils.hasLength(matcher.group(3))) matcher.group(3) else matcher.group(4),
                if (StringUtils.hasLength(matcher.group(5))) matcher.group(5).toInt() else 0, input
            )
        }
    }
}
