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

import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.springframework.boot.build.bom.ResolvedBom.Bom
import org.springframework.boot.build.bom.ResolvedBom.JavadocLink
import org.springframework.boot.build.xml.XmlDocument
import org.w3c.dom.NodeList
import java.io.File
import java.net.URI
import java.util.function.Function
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

/**
 * Creates a [resolved bom][ResolvedBom].
 * 
 * @author Andy Wilkinson
 */
class BomResolver(
    val configurations: ConfigurationContainer,
    val dependencies: DependencyHandler
) {
    val documentBuilder: DocumentBuilder

    init {
        this.documentBuilder = XmlDocument.builder()
    }

    fun resolve(bomExtension: BomExtension): ResolvedBom {
        val libraries: MutableList<ResolvedBom.ResolvedLibrary?> = ArrayList<ResolvedBom.ResolvedLibrary?>()
        for (library in bomExtension.libraries) {
            val managedDependencies: MutableList<ResolvedBom.Id?> = ArrayList<ResolvedBom.Id?>()
            val imports: MutableList<Bom?> = ArrayList<Bom?>()
            for (group in library.groups) {
                for (module in group.modules) {
                    val id =
                        ResolvedBom.Id(group.id, module.name, library.version.version.toString())
                    managedDependencies.add(id)
                }
                for (imported in group.boms) {
                    val bom = bomFrom(
                        resolveBom(
                            "%s:%s:%s".format(group.id, imported.name, library.version.version)
                        )
                    )
                    imports.add(bom)
                }
            }
            val javadocLinks = javadocLinksOf(library).stream()
                .map<JavadocLink> { link: Library.Link? ->
                    JavadocLink(
                        URI.create(link!!.url(library)),
                        link.packages
                    )
                }
                .toList()
            val resolvedLibrary = ResolvedBom.ResolvedLibrary(
                library.name,
                library.version.version.toString(), library.versionProperty, managedDependencies,
                imports, ResolvedBom.Links(javadocLinks)
            )
            libraries.add(resolvedLibrary)
        }
        val idComponents: Array<String?> =
            bomExtension.id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return ResolvedBom(ResolvedBom.Id(idComponents[0], idComponents[1], idComponents[2]), libraries)
    }

    fun javadocLinksOf(library: Library): MutableList<Library.Link?> {
        val javadocLinks = library.getLinks("javadoc")
        return if (javadocLinks != null) javadocLinks else mutableListOf<Library.Link?>()
    }

    fun resolveMavenBom(coordinates: String?): Bom {
        return bomFrom(resolveBom(coordinates))
    }

    fun resolveBom(coordinates: String?): File {
        val artifacts: MutableSet<ResolvedArtifact?> = this.configurations
            .detachedConfiguration(this.dependencies.create(coordinates + "@pom"))
            .getResolvedConfiguration()
            .getResolvedArtifacts()
        check(artifacts.size == 1) {
            "Expected a single artifact but '%s' resolved to %d artifacts"
                .format(coordinates, artifacts.size)
        }
        return artifacts.iterator().next()!!.getFile()
    }

    fun bomFrom(bomFile: File?): Bom {
        try {
            val bom = nodeFrom(bomFile)
            val parentBomFile = parentBomFile(bom)
            var parent: Bom? = null
            if (parentBomFile != null) {
                parent = bomFrom(parentBomFile)
            }
            val properties: Properties =
                Properties.Companion.from(bom, Function { coordinates: String? -> this.nodeFrom(coordinates) })
            val dependencyNodes = bom.nodesAt("/project/dependencyManagement/dependencies/dependency")
            val managedDependencies: MutableList<ResolvedBom.Id?> = ArrayList<ResolvedBom.Id?>()
            val imports: MutableList<Bom?> = ArrayList<Bom?>()
            for (dependency in dependencyNodes) {
                val groupId = properties.replace(dependency.textAt("groupId"))
                val artifactId = properties.replace(dependency.textAt("artifactId"))
                val version = properties.replace(dependency.textAt("version"))
                val classifier = properties.replace(dependency.textAt("classifier"))
                val scope = properties.replace(dependency.textAt("scope"))
                var importedBom: Bom? = null
                if ("import" == scope) {
                    val type = properties.replace(dependency.textAt("type"))
                    if ("pom" == type) {
                        importedBom = bomFrom(resolveBom(groupId + ":" + artifactId + ":" + version))
                    }
                }
                if (importedBom != null) {
                    imports.add(importedBom)
                } else {
                    managedDependencies.add(ResolvedBom.Id(groupId, artifactId, version, classifier))
                }
            }
            var groupId = bom.textAt("/project/groupId")
            if ((groupId == null || groupId.isEmpty()) && parent != null) {
                groupId = parent.id.groupId
            }
            val artifactId = bom.textAt("/project/artifactId")
            var version = bom.textAt("/project/version")
            if ((version == null || version.isEmpty()) && parent != null) {
                version = parent.id.version
            }
            return Bom(ResolvedBom.Id(groupId, artifactId, version), parent, managedDependencies, imports)
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    fun nodeFrom(coordinates: String?): Node {
        return nodeFrom(resolveBom(coordinates))
    }

    fun nodeFrom(bomFile: File?): Node {
        try {
            val document = this.documentBuilder.parse(bomFile)
            return Node(document)
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    fun parentBomFile(bom: Node): File? {
        val parent = bom.nodeAt("/project/parent")
        if (parent != null) {
            val parentGroupId = parent.textAt("groupId")
            val parentArtifactId = parent.textAt("artifactId")
            val parentVersion = parent.textAt("version")
            return resolveBom(parentGroupId + ":" + parentArtifactId + ":" + parentVersion)
        }
        return null
    }

    class Node(
        val delegate: org.w3c.dom.Node,
        val xpath: XPath = XPathFactory.newInstance().newXPath()
    ) {
        fun textAt(expression: String?): String? {
            val text = evaluate(expression + "/text()", XPathConstants.STRING) as String?
            return if (text != null && !text.isBlank()) text else null
        }

        fun nodeAt(expression: String?): Node? {
            val result = evaluate(expression, XPathConstants.NODE) as org.w3c.dom.Node?
            return if (result != null) Node(result, this.xpath) else null
        }

        fun nodesAt(expression: String?): MutableList<Node> {
            val nodes = evaluate(expression, XPathConstants.NODESET) as NodeList
            val things: MutableList<Node> = ArrayList<Node>(nodes.getLength())
            for (i in 0..<nodes.getLength()) {
                things.add(Node(nodes.item(i), this.xpath))
            }
            return things
        }

        fun evaluate(expression: String?, type: QName?): Any? {
            try {
                return this.xpath.evaluate(expression, this.delegate, type)
            } catch (ex: XPathExpressionException) {
                throw RuntimeException(ex)
            }
        }

        fun name(): String {
            return this.delegate.getNodeName()
        }

        fun textContent(): String? {
            return this.delegate.getTextContent()
        }
    }

    class Properties(val properties: MutableMap<String?, String?>) {
        fun replace(input: String?): String? {
            if (input != null && input.startsWith("\${") && input.endsWith("}")) {
                val value = this.properties.get(input)
                if (value != null) {
                    return replace(value)
                }
                throw IllegalStateException("No replacement for " + input)
            }
            return input
        }

        companion object {
            fun from(bom: Node?, resolver: Function<String?, Node?>): Properties {
                try {
                    val properties: MutableMap<String?, String?> = HashMap<String?, String?>()
                    var current = bom
                    while (current != null) {
                        val groupId = current.textAt("/project/groupId")
                        if (groupId != null && !groupId.isEmpty()) {
                            properties.putIfAbsent("\${project.groupId}", groupId)
                        }
                        val version = current.textAt("/project/version")
                        if (version != null && !version.isEmpty()) {
                            properties.putIfAbsent("\${project.version}", version)
                        }
                        val propertyNodes = current.nodesAt("/project/properties/*")
                        for (property in propertyNodes) {
                            properties.putIfAbsent("\${%s}".format(property.name()), property.textContent())
                        }
                        current = parent(current, resolver)
                    }
                    return Properties(properties)
                } catch (ex: Exception) {
                    throw RuntimeException(ex)
                }
            }

            fun parent(current: Node, resolver: Function<String?, Node?>): Node? {
                val parent = current.nodeAt("/project/parent")
                if (parent != null) {
                    val parentGroupId = parent.textAt("groupId")
                    val parentArtifactId = parent.textAt("artifactId")
                    val parentVersion = parent.textAt("version")
                    return resolver.apply(parentGroupId + ":" + parentArtifactId + ":" + parentVersion)
                }
                return null
            }
        }
    }
}
