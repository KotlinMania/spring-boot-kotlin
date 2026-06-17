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
package org.springframework.boot.build.assertj

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.AssertProvider
import org.assertj.core.api.StringAssert
import org.springframework.boot.build.xml.XmlDocument.parse
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

/**
 * AssertJ [AssertProvider] for [Node] assertions.
 * 
 * @author Andy Wilkinson
 */
class NodeAssert(actual: Node?) : AbstractAssert<NodeAssert?, Node?>(actual, NodeAssert::class.java),
    AssertProvider<NodeAssert?> {
    private val xpathFactory: XPathFactory = XPathFactory.newInstance()

    private val xpath: XPath = this.xpathFactory.newXPath()

    constructor(xmlFile: File?) : this(read(xmlFile))

    fun nodeAtPath(xpath: String?): NodeAssert {
        try {
            return NodeAssert(this.xpath.evaluate(xpath, this.actual, XPathConstants.NODE) as Node?)
        } catch (ex: XPathExpressionException) {
            throw RuntimeException(ex)
        }
    }

    fun textAtPath(xpath: String?): StringAssert {
        try {
            return StringAssert(
                this.xpath.evaluate(xpath + "/text()", this.actual, XPathConstants.STRING) as String?
            )
        } catch (ex: XPathExpressionException) {
            throw RuntimeException(ex)
        }
    }

    override fun assertThat(): NodeAssert {
        return this
    }

    companion object {
        private fun read(xmlFile: File?): Document? {
            try {
                return parse(xmlFile)
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }
        }
    }
}
