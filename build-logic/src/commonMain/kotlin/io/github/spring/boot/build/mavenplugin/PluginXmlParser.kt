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
package org.springframework.boot.build.mavenplugin

import org.springframework.boot.build.xml.XmlDocument
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

/**
 * A parser for a Maven plugin's `plugin.xml` file.
 * 
 * @author Andy Wilkinson
 * @author Mike Smithson
 */
class PluginXmlParser {
    private val xpath: XPath

    init {
        this.xpath = XPathFactory.newInstance().newXPath()
    }

    fun parse(pluginXml: File?): Plugin {
        try {
            val root: Node? = XmlDocument.parse(pluginXml)
            val mojos = parseMojos(root)
            return PluginXmlParser.Plugin(
                textAt("//plugin/groupId", root), textAt("//plugin/artifactId", root),
                textAt("//plugin/version", root), textAt("//plugin/goalPrefix", root), mojos
            )
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    @Throws(XPathExpressionException::class)
    private fun textAt(path: String?, source: Node?): String? {
        val text = this.xpath.evaluate(path + "/text()", source)
        return if (text.isEmpty()) null else text
    }

    @Throws(XPathExpressionException::class)
    private fun parseMojos(plugin: Node?): MutableList<Mojo?> {
        val mojos: MutableList<Mojo?> = ArrayList<Mojo?>()
        for (mojoNode in nodesAt("//plugin/mojos/mojo", plugin)) {
            mojos.add(
                Mojo(
                    textAt("goal", mojoNode), format(textAt("description", mojoNode)!!),
                    parseParameters(mojoNode)
                )
            )
        }
        return mojos
    }

    @Throws(XPathExpressionException::class)
    private fun nodesAt(path: String?, source: Node?): Iterable<Node> {
        return IterableNodeList.Companion.of(this.xpath.evaluate(path, source, XPathConstants.NODESET) as NodeList?)
    }

    @Throws(XPathExpressionException::class)
    private fun parseParameters(mojoNode: Node?): MutableList<Parameter?> {
        val defaultValues: MutableMap<String?, String?> = HashMap<String?, String?>()
        val userProperties: MutableMap<String?, String?> = HashMap<String?, String?>()
        for (parameterConfigurationNode in nodesAt("configuration/*", mojoNode)) {
            val userProperty = parameterConfigurationNode.getTextContent()
            if (userProperty != null && !userProperty.isEmpty()) {
                userProperties.put(
                    parameterConfigurationNode.getNodeName(),
                    userProperty.replace("\${", "`").replace("}", "`")
                )
            }
            val defaultValueAttribute = parameterConfigurationNode.getAttributes().getNamedItem("default-value")
            if (defaultValueAttribute != null && !defaultValueAttribute.getTextContent().isEmpty()) {
                defaultValues.put(parameterConfigurationNode.getNodeName(), defaultValueAttribute.getTextContent())
            }
        }
        val parameters: MutableList<Parameter?> = ArrayList<Parameter?>()
        for (parameterNode in nodesAt("parameters/parameter", mojoNode)) {
            parameters.add(parseParameter(parameterNode, defaultValues, userProperties))
        }
        return parameters
    }

    @Throws(XPathExpressionException::class)
    private fun parseParameter(
        parameterNode: Node?, defaultValues: MutableMap<String?, String?>,
        userProperties: MutableMap<String?, String?>
    ): Parameter {
        val description = textAt("description", parameterNode)
        return PluginXmlParser.Parameter(
            textAt("name", parameterNode), textAt("type", parameterNode),
            booleanAt("required", parameterNode), booleanAt("editable", parameterNode),
            if (description != null) format(description) else "", defaultValues.get(textAt("name", parameterNode)),
            userProperties.get(textAt("name", parameterNode)), textAt("since", parameterNode)
        )
    }

    @Throws(XPathExpressionException::class)
    private fun booleanAt(path: String?, node: Node?): Boolean {
        return textAt(path, node).toBoolean()
    }

    private fun format(input: String): String {
        return input.replace("<code>", "`")
            .replace("</code>", "`")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("<br>", " ")
            .replace("<p>", " ")
            .replace("\n", " ")
            .replace("&quot;", "\"")
            .replace("\\{@code (.*?)}".toRegex(), "`$1`")
            .replace("\\{@link (.*?)}".toRegex(), "`$1`")
            .replace("\\{@literal (.*?)}".toRegex(), "`$1`")
            .replace("<a href=.\"(.*?)\".>(.*?)</a>".toRegex(), "$1[$2]")
    }

    private class IterableNodeList(private val nodeList: NodeList) : Iterable<Node?> {
        override fun iterator(): MutableIterator<Node?> {
            return object : MutableIterator<Node?> {
                private var index = 0

                override fun hasNext(): Boolean {
                    return this.index < this@IterableNodeList.nodeList.getLength()
                }

                override fun next(): Node? {
                    return this@IterableNodeList.nodeList.item(this.index++)
                }
            }
        }

        companion object {
            private fun of(nodeList: NodeList): Iterable<Node> {
                return IterableNodeList(nodeList)
            }
        }
    }

    class Plugin private constructor(
        val groupId: String?,
        val artifactId: String?,
        val version: String?,
        val goalPrefix: String?,
        val mojos: MutableList<Mojo?>?
    )

    class Mojo private constructor(
        val goal: String?,
        val description: String?,
        val parameters: MutableList<Parameter?>?
    )

    class Parameter private constructor(
        val name: String?,
        val type: String?,
        val isRequired: Boolean,
        val isEditable: Boolean,
        val description: String?,
        val defaultValue: String?,
        val userProperty: String?,
        val since: String?
    )
}
