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

import org.apache.maven.artifact.versioning.ComparableVersion

/**
 * Base class for [DependencyVersion] implementations.
 * 
 * @author Andy Wilkinson
 */
internal abstract class AbstractDependencyVersion protected constructor(private val comparableVersion: ComparableVersion) :
    DependencyVersion {
    override fun compareTo(other: DependencyVersion): Int {
        val otherComparable =
            if (other is AbstractDependencyVersion) other.comparableVersion else ComparableVersion(other.toString())
        return this.comparableVersion.compareTo(otherComparable)
    }

    override fun isUpgrade(candidate: DependencyVersion, movingToSnapshots: Boolean): Boolean {
        val comparableCandidate =
            if (candidate is AbstractDependencyVersion) candidate.comparableVersion else ComparableVersion(candidate.toString())
        return comparableCandidate.compareTo(this.comparableVersion) > 0
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
        val other = obj as AbstractDependencyVersion
        return this.comparableVersion == other.comparableVersion
    }

    override fun hashCode(): Int {
        return this.comparableVersion.hashCode()
    }

    override fun toString(): String {
        return this.comparableVersion.toString()
    }
}
