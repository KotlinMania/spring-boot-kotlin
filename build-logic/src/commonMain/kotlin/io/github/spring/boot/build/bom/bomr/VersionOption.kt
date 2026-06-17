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
package org.springframework.boot.build.bom.bomr

import org.springframework.boot.build.bom.Library
import org.springframework.boot.build.bom.Library.LibraryVersion
import org.springframework.boot.build.bom.Library.VersionAlignment
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import org.springframework.util.StringUtils

/**
 * An option for a library update.
 * 
 * @author Andy Wilkinson
 */
open class VersionOption(val version: DependencyVersion) {
    override fun toString(): String {
        return this.version.toString()
    }

    open fun upgrade(library: Library): Upgrade {
        return Upgrade(library, library.withVersion(LibraryVersion(this.version)))
    }

    class AlignedVersionOption(version: DependencyVersion, private val alignedWith: VersionAlignment?) :
        VersionOption(version) {
        public override fun toString(): String {
            return super.toString() + " (aligned with " + this.alignedWith + ")"
        }
    }

    class ResolvedVersionOption(version: DependencyVersion, private val missingModules: MutableList<String?>) :
        VersionOption(version) {
        public override fun toString(): String {
            if (this.missingModules.isEmpty()) {
                return super.toString()
            }
            return (super.toString() + " (some modules are missing: "
                    + StringUtils.collectionToDelimitedString(this.missingModules, ", ") + ")")
        }
    }

    class SnapshotVersionOption(version: DependencyVersion, private val releaseVersion: DependencyVersion) :
        VersionOption(version) {
        public override fun toString(): String {
            return super.toString() + " (for " + this.releaseVersion + ")"
        }

        override fun upgrade(library: Library): Upgrade {
            return Upgrade(
                library, library.withVersion(LibraryVersion(super.version)),
                library.withVersion(LibraryVersion(this.releaseVersion))
            )
        }
    }
}
