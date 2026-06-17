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
package org.springframework.boot.build.bom

import groovy.namespace.QName
import groovy.util.Node
import org.gradle.api.*
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlatformExtension
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.springframework.boot.build.MavenRepositoryPlugin
import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import java.util.*
import java.util.stream.Collectors

/**
 * [Plugin] for defining a bom. Dependencies are added as constraints in the
 * `api` configuration. Imported boms are added as enforced platforms in the
 * `api` configuration.
 * 
 * @author Andy Wilkinson
 */
class BomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val plugins = project.plugins
        plugins.apply<MavenRepositoryPlugin>(MavenRepositoryPlugin::class.java)
        plugins.apply<JavaPlatformPlugin>(JavaPlatformPlugin::class.java)
        val javaPlatform = project.getExtensions().getByType<JavaPlatformExtension>(JavaPlatformExtension::class.java)
        javaPlatform.allowDependencies()
        createApiEnforcedConfiguration(project)
        val bom = project.getExtensions().create<BomExtension>("bom", BomExtension::class.java, project)
        val createResolvedBom = project.getTasks()
            .register<CreateResolvedBom>("createResolvedBom", CreateResolvedBom::class.java, bom)
        val checkBom = project.getTasks().register<CheckBom>("bomrCheck", CheckBom::class.java, bom)
        checkBom.configure(
            Action { task: CheckBom ->
                task!!.resolvedBomFile
                    .set(createResolvedBom.flatMap<RegularFile>(Transformer { obj: CreateResolvedBom? -> obj!!.outputFile }))
            })
        project.getTasks().named("check").configure(Action { check: Task -> check!!.dependsOn(checkBom) })
        project.getTasks().register<CheckLinks>("checkLinks", CheckLinks::class.java, bom)
        val resolvedBomConfiguration = project.getConfigurations().create("resolvedBom")
        project.getArtifacts()
            .add(
                resolvedBomConfiguration.name,
                createResolvedBom.map<RegularFileProperty>(Transformer { obj: CreateResolvedBom? -> obj!!.outputFile })) { artifact: ConfigurablePublishArtifact -> artifact!!.builtBy(createResolvedBom) }
        PublishingCustomizer(project, bom).customize()
    }

    private fun createApiEnforcedConfiguration(project: Project) {
        val apiEnforced = project.getConfigurations()
            .create(API_ENFORCED_CONFIGURATION_NAME) { configuration: Configuration ->
                configuration.setCanBeConsumed(false)
                configuration.setCanBeResolved(false)
                configuration.setVisible(false)
            }
        project.getConfigurations()
            .getByName(JavaPlatformPlugin.ENFORCED_API_ELEMENTS_CONFIGURATION_NAME)
            .extendsFrom(apiEnforced)
        project.getConfigurations()
            .getByName(JavaPlatformPlugin.ENFORCED_RUNTIME_ELEMENTS_CONFIGURATION_NAME)
            .extendsFrom(apiEnforced)
    }

    private class PublishingCustomizer(private val project: Project, private val bom: BomExtension) {
        fun customize() {
            val publishing =
                this.project.getExtensions().getByType<PublishingExtension>(PublishingExtension::class.java)
            publishing.publications.withType<MavenPublication>(MavenPublication::class.java)
                .all(Action { publication: MavenPublication -> this.configurePublication(publication) })
        }

        fun configurePublication(publication: MavenPublication) {
            publication.pom(Action { pom: MavenPom -> this.customizePom(pom) })
        }

        fun customizePom(pom: MavenPom) {
            pom.withXml(Action { xml: XmlProvider ->
                val projectNode = xml!!.asNode()
                val properties = Node(null, "properties")
                this.bom.properties
                    .forEach { (name: String?, value: DependencyVersion?) -> properties.appendNode(name, value) }
                val dependencyManagement = findChild(projectNode, "dependencyManagement")
                if (dependencyManagement != null) {
                    addPropertiesBeforeDependencyManagement(projectNode, properties)
                    addClassifiedManagedDependencies(dependencyManagement)
                    replaceVersionsWithVersionPropertyReferences(dependencyManagement)
                    addExclusionsToManagedDependencies(dependencyManagement)
                    addTypesToManagedDependencies(dependencyManagement)
                } else {
                    projectNode.children().add(properties)
                }
                addPluginManagement(projectNode)
            })
        }

        fun addPropertiesBeforeDependencyManagement(projectNode: Node, properties: Node?) {
            for (i in projectNode.children().indices) {
                if (isNodeWithName(projectNode.children().get(i), "dependencyManagement")) {
                    projectNode.children().add(i, properties)
                    break
                }
            }
        }

        fun replaceVersionsWithVersionPropertyReferences(dependencyManagement: Node) {
            val dependencies = findChild(dependencyManagement, "dependencies")
            if (dependencies != null) {
                for (dependency in findChildren(dependencies, "dependency")) {
                    val groupId = findChild(dependency, "groupId")!!.text()
                    val artifactId = findChild(dependency, "artifactId")!!.text()
                    val classifierNode = findChild(dependency, "classifier")
                    val classifier = if (classifierNode != null) classifierNode.text() else ""
                    val versionProperty = this.bom.getArtifactVersionProperty(groupId, artifactId, classifier)
                    if (versionProperty != null) {
                        findChild(dependency, "version")!!.setValue("\${" + versionProperty + "}")
                    }
                }
            }
        }

        fun addExclusionsToManagedDependencies(dependencyManagement: Node) {
            val dependencies = findChild(dependencyManagement, "dependencies")
            if (dependencies != null) {
                for (dependency in findChildren(dependencies, "dependency")) {
                    val groupId = findChild(dependency, "groupId")!!.text()
                    val artifactId = findChild(dependency, "artifactId")!!.text()
                    this.bom.libraries
                        .stream()
                        .flatMap<Library.Group> { library: Library? -> library!!.groups.stream() }
                        .filter { group: Library.Group? -> group!!.id == groupId }
                        .flatMap<Library.Module> { group: Library.Group? -> group!!.modules.stream() }
                        .filter { module: Library.Module? -> module!!.name == artifactId }
                        .flatMap<Library.Exclusion> { module: Library.Module? -> module!!.exclusions.stream() }
                        .forEach { exclusion: Library.Exclusion? ->
                            val exclusions = findOrCreateNode(dependency, "exclusions")
                            val node = Node(exclusions, "exclusion")
                            node.appendNode("groupId", exclusion!!.groupId)
                            node.appendNode("artifactId", exclusion.artifactId)
                        }
                }
            }
        }

        fun addTypesToManagedDependencies(dependencyManagement: Node) {
            val dependencies = findChild(dependencyManagement, "dependencies")
            if (dependencies != null) {
                for (dependency in findChildren(dependencies, "dependency")) {
                    val groupId = findChild(dependency, "groupId")!!.text()
                    val artifactId = findChild(dependency, "artifactId")!!.text()
                    val types = this.bom.libraries
                        .stream()
                        .flatMap<Library.Group> { library: Library? -> library!!.groups.stream() }
                        .filter { group: Library.Group? -> group!!.id == groupId }
                        .flatMap<Library.Module> { group: Library.Group? -> group!!.modules.stream() }
                        .filter { module: Library.Module? -> module!!.name == artifactId }
                        .map<String> { obj: Library.Module? -> obj!!.type }
                        .filter { obj: String? -> Objects.nonNull(obj) }
                        .collect(Collectors.toSet())
                    check(types.size <= 1) { "Multiple types for " + groupId + ":" + artifactId + ": " + types }
                    if (types.size == 1) {
                        val type = types.iterator().next()
                        dependency.appendNode("type", type)
                    }
                }
            }
        }

        fun addClassifiedManagedDependencies(dependencyManagement: Node) {
            val dependencies = findChild(dependencyManagement, "dependencies")
            if (dependencies != null) {
                for (dependency in findChildren(dependencies, "dependency")) {
                    val groupId = findChild(dependency, "groupId")!!.text()
                    val artifactId = findChild(dependency, "artifactId")!!.text()
                    val version = findChild(dependency, "version")!!.text()
                    val classifiers = this.bom.libraries
                        .stream()
                        .flatMap<Library.Group> { library: Library? -> library!!.groups.stream() }
                        .filter { group: Library.Group? -> group!!.id == groupId }
                        .flatMap<Library.Module> { group: Library.Group? -> group!!.modules.stream() }
                        .filter { module: Library.Module? -> module!!.name == artifactId }
                        .map<String> { obj: Library.Module? -> obj!!.classifier }
                        .filter { obj: String? -> Objects.nonNull(obj) }
                        .collect(Collectors.toSet())
                    var target: Node? = dependency
                    for (classifier in classifiers) {
                        if (!classifier.isEmpty()) {
                            if (target == null) {
                                target = Node(null, "dependency")
                                target.appendNode("groupId", groupId)
                                target.appendNode("artifactId", artifactId)
                                target.appendNode("version", version)
                                val index = dependency.parent().children().indexOf(dependency)
                                dependency.parent().children().add(index + 1, target)
                            }
                            target.appendNode("classifier", classifier)
                        }
                        target = null
                    }
                }
            }
        }

        fun addPluginManagement(projectNode: Node) {
            for (library in this.bom.libraries) {
                for (group in library.groups!!) {
                    val plugins = findOrCreateNode(projectNode, "build", "pluginManagement", "plugins")
                    for (pluginName in group.plugins!!) {
                        val plugin = Node(plugins, "plugin")
                        plugin.appendNode("groupId", group.id)
                        plugin.appendNode("artifactId", pluginName)
                        val versionProperty = library.versionProperty
                        val value: String? = if (versionProperty != null)
                            "\${" + versionProperty + "}"
                        else
                            library.version.version.toString()
                        plugin.appendNode("version", value)
                    }
                }
            }
        }

        fun findOrCreateNode(parent: Node, vararg path: String): Node {
            var current: Node? = parent
            for (nodeName in path) {
                var child = findChild(current!!, nodeName)
                if (child == null) {
                    child = Node(current, nodeName)
                }
                current = child
            }
            return current!!
        }

        fun findChild(parent: Node, name: String): Node? {
            for (child in parent.children()) {
                if (isNodeWithName(child, name)) {
                    return child as Node
                }
            }
            return null
        }

        fun findChildren(parent: Node, name: String): MutableList<Node> {
            return parent.children().stream().filter { child: Any? -> isNodeWithName(child, name) }.toList()
        }

        fun isNodeWithName(candidate: Any?, name: String): Boolean {
            if (candidate is Node) {
                if ((candidate.name() is QName) && name == qname.getLocalPart()) {
                    return true
                }
                return name == candidate.name()
            }
            return false
        }
    }

    companion object {
        const val API_ENFORCED_CONFIGURATION_NAME: String = "apiEnforced"
    }
}
