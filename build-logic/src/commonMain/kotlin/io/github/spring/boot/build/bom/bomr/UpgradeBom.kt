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

import org.gradle.api.Action
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.springframework.boot.build.bom.BomExtension
import org.springframework.boot.build.properties.BuildProperties
import org.springframework.boot.build.properties.BuildType
import javax.inject.Inject

/**
 * [Task] to upgrade the libraries managed by a bom.
 * 
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
abstract class UpgradeBom @Inject constructor(bom: BomExtension?) : UpgradeDependencies(bom) {
    init {
        when (BuildProperties.get(project).buildType) {
            BuildType.OPEN_SOURCE -> addOpenSourceRepositories(project.getRepositories())
            BuildType.COMMERCIAL -> addCommercialRepositories()
        }
    }

    private fun addOpenSourceRepositories(repositories: RepositoryHandler) {
        repositoryNames.add(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME)
        repositories.withType<MavenArtifactRepository>(
            MavenArtifactRepository::class.java) { repository: MavenArtifactRepository ->
                val name = repository!!.name
                if (name.startsWith("spring-") && !name.endsWith("-snapshot")) {
                    repositoryNames.add(name)
                }
            }
    }

    private fun addCommercialRepositories() {
        repositoryNames.addAll(
            ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME,
            "spring-commercial-release"
        )
    }

    override fun commitMessage(upgrade: Upgrade, issueNumber: Int): String {
        return issueTitle(upgrade) + "\n\nCloses gh-" + issueNumber
    }

    override fun upgradesApplied(upgrades: MutableList<Upgrade>) {
        if (upgrades.isEmpty()) {
            return
        }
        println()
        println("Upgrade release notes:")
        println()
        for (upgrade in upgrades) {
            val library = upgrade.toRelease
            val releaseNotes = library.getLinkUrl("releaseNotes")
            if (releaseNotes != null) {
                println("* " + releaseNotes + "[" + library.nameAndVersion + "]")
            } else {
                println("* " + library.nameAndVersion)
            }
        }
        println()
    }
}
