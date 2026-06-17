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

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.regex.Pattern

/**
 * `UpgradeApplicator` is used to apply an [Upgrade]. Modifies the bom
 * configuration in the build file or a version property in `gradle.properties`.
 * 
 * @author Andy Wilkinson
 */
internal class UpgradeApplicator(private val buildFile: Path, private val gradleProperties: Path) {
    @Throws(IOException::class)
    fun apply(upgrade: Upgrade): Path {
        val buildFileContents = Files.readString(this.buildFile)
        val toName = upgrade.to.name
        var matcher = Pattern.compile("library\\(\"" + toName + "\", \"(.+)\"\\)").matcher(buildFileContents)
        if (!matcher.find()) {
            matcher = Pattern.compile("library\\(\"" + toName + "\"\\) \\{\\s+version\\(\"(.+)\"\\)", Pattern.MULTILINE)
                .matcher(buildFileContents)
            check(matcher.find()) {
                ("Failed to find definition for library '" + upgrade.to.name
                        + "' in bom '" + this.buildFile + "'")
            }
        }
        val version = matcher.group(1)
        if (version.startsWith("\${") && version.endsWith("}")) {
            updateGradleProperties(upgrade, version)
            return this.gradleProperties
        } else {
            updateBuildFile(upgrade, buildFileContents, matcher.start(1), matcher.end(1))
            return this.buildFile
        }
    }

    @Throws(IOException::class)
    private fun updateGradleProperties(upgrade: Upgrade, version: String) {
        val property = version.substring(2, version.length - 1)
        val gradlePropertiesContents = Files.readString(this.gradleProperties)
        val modified = gradlePropertiesContents.replace(
            property + "=" + upgrade.from.version,
            property + "=" + upgrade.to.version
        )
        overwrite(this.gradleProperties, modified)
    }

    @Throws(IOException::class)
    private fun updateBuildFile(upgrade: Upgrade, buildFileContents: String, versionStart: Int, versionEnd: Int) {
        val modified = (buildFileContents.substring(0, versionStart) + upgrade.to.version
                + buildFileContents.substring(versionEnd))
        overwrite(this.buildFile, modified)
    }

    @Throws(IOException::class)
    private fun overwrite(target: Path, content: String) {
        Files.writeString(target, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
    }
}
