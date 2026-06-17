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
import java.util.concurrent.Callable

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
internal class MavenPublishingConventions {
    fun apply(project: Project) {
        project.getPlugins().withType<MavenPublishPlugin?>(MavenPublishPlugin::class.java)
            .all(Action { mavenPublish: MavenPublishPlugin? ->
                val publishing = project.getExtensions().getByType<PublishingExtension>(PublishingExtension::class.java)
                if (project.hasProperty("deploymentRepository")) {
                    publishing.getRepositories().maven(Action { mavenRepository: MavenArtifactRepository? ->
                        mavenRepository!!.setUrl(project.property("deploymentRepository")!!)
                        mavenRepository.setName("deployment")
                    })
                }
                publishing.getPublications()
                    .withType<MavenPublication?>(MavenPublication::class.java)
                    .all(Action { mavenPublication: MavenPublication? ->
                        customizeMavenPublication(
                            mavenPublication!!,
                            project
                        )
                    })
                project.getPlugins().withType<JavaPlugin?>(JavaPlugin::class.java)
                    .all(Action { javaPlugin: JavaPlugin? ->
                        val extension =
                            project.getExtensions().getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
                        extension.withJavadocJar()
                        extension.withSourcesJar()
                    })
            })
    }

    private fun customizeMavenPublication(publication: MavenPublication, project: Project) {
        customizePom(publication.getPom(), project)
        project.getPlugins()
            .withType<JavaPlugin?>(JavaPlugin::class.java)
            .all(Action { javaPlugin: JavaPlugin? -> customizeJavaMavenPublication(publication, project) })
    }

    private fun customizePom(pom: MavenPom, project: Project) {
        pom.getUrl().set("https://spring.io/projects/spring-boot")
        pom.getName().set(project.provider<String?>(Callable { project.getName() }))
        pom.getDescription().set(project.provider<String?>(Callable { project.getDescription() }))
        if (!isUserInherited(project)) {
            pom.organization(Action { organization: MavenPomOrganization? -> this.customizeOrganization(organization) })
        }
        pom.licenses(Action { licences: MavenPomLicenseSpec? -> this.customizeLicences(licences) })
        pom.developers(Action { developers: MavenPomDeveloperSpec? -> this.customizeDevelopers(developers) })
        pom.scm(Action { scm: MavenPomScm? -> customizeScm(scm!!, project) })
        pom.issueManagement(Action { issueManagement: MavenPomIssueManagement? ->
            customizeIssueManagement(
                issueManagement!!,
                project
            )
        })
    }

    private fun customizeJavaMavenPublication(publication: MavenPublication, project: Project?) {
        publication.versionMapping(Action { strategy: VersionMappingStrategy? ->
            strategy!!.usage(Usage.JAVA_API, Action { mappingStrategy: VariantVersionMappingStrategy? ->
                mappingStrategy!!
                    .fromResolutionOf(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            })
        })
        publication.versionMapping(
            Action { strategy: VersionMappingStrategy? ->
                strategy!!.usage(
                    Usage.JAVA_RUNTIME,
                    Action { obj: VariantVersionMappingStrategy? -> obj!!.fromResolutionResult() })
            })
    }

    private fun customizeOrganization(organization: MavenPomOrganization) {
        organization.getName().set("VMware, Inc.")
        organization.getUrl().set("https://spring.io")
    }

    private fun customizeLicences(licences: MavenPomLicenseSpec) {
        licences.license(Action { licence: MavenPomLicense? ->
            licence!!.getName().set("Apache License, Version 2.0")
            licence.getUrl().set("https://www.apache.org/licenses/LICENSE-2.0")
        })
    }

    private fun customizeDevelopers(developers: MavenPomDeveloperSpec) {
        developers.developer(Action { developer: MavenPomDeveloper? ->
            developer!!.getName().set("Spring")
            developer.getEmail().set("ask@spring.io")
            developer.getOrganization().set("VMware, Inc.")
            developer.getOrganizationUrl().set("https://www.spring.io")
        })
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
        return "spring-boot-starter-parent" == project.getName()
                || "spring-boot-dependencies" == project.getName()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenPublishingConventions::class.java)
    }
}
