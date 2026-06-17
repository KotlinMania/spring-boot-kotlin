/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.boot.build.xml

import org.w3c.dom.Document
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * XML [Document] builder and parsing.
 * 
 * @author Phillip Webb
 * @author Sebastien Tardif
 */
object XmlDocument {
    private val factory: DocumentBuilderFactory

    init {
        try {
            factory = DocumentBuilderFactory.newInstance()
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        } catch (ex: ParserConfigurationException) {
            throw IllegalStateException(ex)
        }
    }

    @Throws(SAXException::class, IOException::class)
    fun parseContent(content: String): Document? {
        return builder()!!.parse(InputSource(StringReader(content)))
    }

    @Throws(SAXException::class, IOException::class)
    fun parse(file: File?): Document? {
        return builder()!!.parse(file)
    }

    fun builder(): DocumentBuilder? {
        try {
            return factory.newDocumentBuilder()
        } catch (ex: ParserConfigurationException) {
            throw IllegalStateException(ex)
        }
    }
}
