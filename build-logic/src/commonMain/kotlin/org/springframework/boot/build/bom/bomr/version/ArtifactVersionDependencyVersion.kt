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
import org.springframework.util.StringUtils
import java.util.*
import java.util.function.Function

/**
 * A [DependencyVersion] backed by an [ArtifactVersion].
 * 
 * @author Andy Wilkinson
 */
internal open class ArtifactVersionDependencyVersion : AbstractDependencyVersion {
    private val artifactVersion: ArtifactVersion

    protected constructor(artifactVersion: ArtifactVersion) : super(ComparableVersion(toNormalizedString(artifactVersion))) {
        this.artifactVersion = artifactVersion
    }

    protected constructor(artifactVersion: ArtifactVersion, comparableVersion: ComparableVersion?) : super(
        comparableVersion
    ) {
        this.artifactVersion = artifactVersion
    }

    override fun isSameMajor(other: DependencyVersion?): Boolean {
        if (other is ReleaseTrainDependencyVersion) {
            return false
        }
        return extractArtifactVersionDependencyVersion(other).map<Boolean?>(Function { other: ArtifactVersionDependencyVersion? ->
            this.isSameMajor(
                other
            )
        }).orElse(true)
    }

    private fun isSameMajor(other: ArtifactVersionDependencyVersion): Boolean {
        return this.artifactVersion.getMajorVersion() == other.artifactVersion.getMajorVersion()
    }

    override fun isSameMinor(other: DependencyVersion?): Boolean {
        if (other is ReleaseTrainDependencyVersion) {
            return false
        }
        return extractArtifactVersionDependencyVersion(other).map<Boolean?>(Function { other: ArtifactVersionDependencyVersion? ->
            this.isSameMinor(
                other
            )
        }).orElse(true)
    }

    private fun isSameMinor(other: ArtifactVersionDependencyVersion): Boolean {
        return isSameMajor(other) && this.artifactVersion.getMinorVersion() == other.artifactVersion.getMinorVersion()
    }

    public override fun isUpgrade(candidate: DependencyVersion?, movingToSnapshots: Boolean): Boolean {
        if (candidate is MultipleComponentsDependencyVersion) {
            return super.isUpgrade(candidate, movingToSnapshots)
        }
        if (candidate !is ArtifactVersionDependencyVersion) {
            return false
        }
        val other = candidate.artifactVersion
        if (this.artifactVersion == other) {
            return false
        }
        if (sameMajorMinorIncremental(other)) {
            if (!StringUtils.hasLength(this.artifactVersion.getQualifier())
                || "RELEASE" == this.artifactVersion.getQualifier()
            ) {
                return false
            }
            if (this.isSnapshot) {
                return true
            } else if (candidate.isSnapshot) {
                return movingToSnapshots
            }
        }
        return super.isUpgrade(candidate, movingToSnapshots)
    }

    private fun sameMajorMinorIncremental(other: ArtifactVersion): Boolean {
        return this.artifactVersion.getMajorVersion() == other.getMajorVersion() && this.artifactVersion.getMinorVersion() == other.getMinorVersion() && this.artifactVersion.getIncrementalVersion() == other.getIncrementalVersion()
    }

    private val isSnapshot: Boolean
        get() = "SNAPSHOT" == this.artifactVersion.getQualifier()
                || "BUILD" == this.artifactVersion.getQualifier()

    override fun isSnapshotFor(candidate: DependencyVersion?): Boolean {
        if (!this.isSnapshot || candidate !is ArtifactVersionDependencyVersion) {
            return false
        }
        return sameMajorMinorIncremental(candidate.artifactVersion)
    }

    public override fun compareTo(other: DependencyVersion?): Int {
        if (other is ArtifactVersionDependencyVersion) {
            val otherArtifactVersion = other.artifactVersion
            if ((this.artifactVersion.getQualifier() != otherArtifactVersion.getQualifier())
                && "snapshot".equals(otherArtifactVersion.getQualifier(), ignoreCase = true)
                && otherArtifactVersion.getMajorVersion() == this.artifactVersion.getMajorVersion() && otherArtifactVersion.getMinorVersion() == this.artifactVersion.getMinorVersion() && otherArtifactVersion.getIncrementalVersion() == this.artifactVersion.getIncrementalVersion()
            ) {
                return 1
            }
        }
        return super.compareTo(other)
    }

    public override fun toString(): String {
        return this.artifactVersion.toString()
    }

    protected fun extractArtifactVersionDependencyVersion(
        other: DependencyVersion?
    ): Optional<ArtifactVersionDependencyVersion?> {
        var artifactVersion: ArtifactVersionDependencyVersion? = null
        if (other is ArtifactVersionDependencyVersion) {
            artifactVersion = other
        }
        return Optional.ofNullable<ArtifactVersionDependencyVersion?>(artifactVersion)
    }

    companion object {
        private fun toNormalizedString(artifactVersion: ArtifactVersion): String {
            val versionString = artifactVersion.toString()
            if (versionString.endsWith(".RELEASE")) {
                return versionString.substring(0, versionString.length - 8)
            }
            if (versionString.endsWith(".BUILD-SNAPSHOT")) {
                return versionString.substring(0, versionString.length - 15) + "-SNAPSHOT"
            }
            return versionString
        }

        fun parse(version: String): ArtifactVersionDependencyVersion? {
            val artifactVersion: ArtifactVersion = DefaultArtifactVersion(version)
            if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier() == version) {
                return null
            }
            return ArtifactVersionDependencyVersion(artifactVersion)
        }
    }
}
