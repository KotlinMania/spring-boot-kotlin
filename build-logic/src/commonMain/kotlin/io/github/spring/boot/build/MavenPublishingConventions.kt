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
package org.springframework.boot.build

import org.gradle.kotlin.dsl.*

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.VariantVersionMappingStrategy
import org.gradle.api.publish.VersionMappingStrategy
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.build.properties.BuildProperties.Companion.get
import org.springframework.boot.build.properties.BuildType

/**
 * Conventions that are applied in the presence of the [MavenPublishPlugin]. When
 * the plugin is applied:
 * 
 * 
 *  * If the `deploymentRepository` property has been set, a
 * [Maven artifact repository][MavenArtifactRepository] is configured to publish to
 * it.
 *  * The poms of all [Maven publications][MavenPublication] are customized to meet
 * Maven Central's requirements.
 *  * If the [Java plugin][JavaPlugin] has also been applied:
 * 
 *  * Creation of Javadoc and source jars is enabled.
 *  * Publication metadata (poms and Gradle module metadata) is configured to use
 * resolved versions.
 * 
 * 
 * 
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author Mike Smithson
 */
class MavenPublishingConventions {
    fun apply(project: Project) {
        project.plugins.withType<MavenPublishPlugin>().configureEach {
            val publishing = project.getExtensions().getByType<PublishingExtension>()
            if (project.hasProperty("deploymentRepository")) {
                publishing.getRepositories().maven {
                    setUrl(project.property("deploymentRepository")!!)
                    setName("deployment")
                }
            }
            publishing.publications.withType<MavenPublication>().configureEach {
                customizeMavenPublication(this, project)
            }
            project.plugins.withType<JavaPlugin>().configureEach {
                val extension = project.getExtensions().getByType<JavaPluginExtension>()
                extension.withJavadocJar()
                extension.withSourcesJar()
            }
        }
    }

    private fun customizeMavenPublication(publication: MavenPublication, project: Project) {
        customizePom(publication.getPom(), project)
        project.plugins.withType<JavaPlugin>().configureEach {
            customizeJavaMavenPublication(publication, project)
        }
    }

    private fun customizePom(pom: MavenPom, project: Project) {
        pom.getUrl().set("https://spring.io/projects/spring-boot")
        pom.name.set(project.provider { project.name })
        pom.description.set(project.provider { project.description })
        if (!isUserInherited(project)) {
            pom.organization { this@MavenPublishingConventions.customizeOrganization(this) }
        }
        pom.licenses { this@MavenPublishingConventions.customizeLicences(this) }
        pom.developers { this@MavenPublishingConventions.customizeDevelopers(this) }
        pom.scm { customizeScm(this, project) }
        pom.issueManagement { customizeIssueManagement(this, project) }
    }

    private fun customizeJavaMavenPublication(publication: MavenPublication, project: Project) {
        publication.versionMapping {
            usage(Usage.JAVA_API) {
                fromResolutionOf(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            }
        }
        publication.versionMapping {
            usage(Usage.JAVA_RUNTIME) { fromResolutionResult() }
        }
    }

    private fun customizeOrganization(organization: MavenPomOrganization) {
        organization.name.set("VMware, Inc.")
        organization.getUrl().set("https://spring.io")
    }

    private fun customizeLicences(licences: MavenPomLicenseSpec) {
        licences.license {
            name.set("Apache License, Version 2.0")
            getUrl().set("https://www.apache.org/licenses/LICENSE-2.0")
        }
    }

    private fun customizeDevelopers(developers: MavenPomDeveloperSpec) {
        developers.developer {
            name.set("Spring")
            getEmail().set("ask@spring.io")
            getOrganization().set("VMware, Inc.")
            getOrganizationUrl().set("https://www.spring.io")
        }
    }

    private fun customizeScm(scm: MavenPomScm, project: Project) {
        if (get(project).buildType != BuildType.OPEN_SOURCE) {
            logger.debug("Skipping Maven POM SCM for non open source build type")
            return
        }
        scm.getUrl().set("https://github.com/spring-projects/spring-boot")
        if (!isUserInherited(project)) {
            scm.getConnection().set("scm:git:git://github.com/spring-projects/spring-boot.git")
            scm.getDeveloperConnection().set("scm:git:ssh://git@github.com/spring-projects/spring-boot.git")
        }
    }

    private fun customizeIssueManagement(issueManagement: MavenPomIssueManagement, project: Project) {
        if (get(project).buildType != BuildType.OPEN_SOURCE) {
            logger.debug("Skipping Maven POM SCM for non open source build type")
            return
        }
        if (!isUserInherited(project)) {
            issueManagement.getSystem().set("GitHub")
            issueManagement.getUrl().set("https://github.com/spring-projects/spring-boot/issues")
        }
    }

    private fun isUserInherited(project: Project): Boolean {
        return "spring-boot-starter-parent" == project.name
                || "spring-boot-dependencies" == project.name
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenPublishingConventions::class.java)
    }
}
