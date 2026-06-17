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
package org.springframework.boot.build.context.properties

import java.util.*
import java.util.stream.Collectors

/**
 * Table row containing a single configuration property.
 * 
 * @author Brian Clozel
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
internal class SingleRow(private val snippets: Snippets?, snippet: Snippet?, property: ConfigurationProperty) :
    Row(snippet, property.getName()) {
    private val defaultValue: String?

    private val property: ConfigurationProperty

    constructor(snippet: Snippet?, property: ConfigurationProperty) : this(null, snippet, property)

    init {
        this.defaultValue = getDefaultValue(property.getDefaultValue())
        this.property = property
    }

    private fun getDefaultValue(defaultValue: Any?): String? {
        if (defaultValue == null) {
            return null
        }
        if (defaultValue.javaClass.isArray()) {
            return Arrays.stream<Any?>(defaultValue as Array<Any?>)
                .map<String?> { obj: Any? -> obj.toString() }
                .collect(Collectors.joining("," + System.lineSeparator()))
        }
        return defaultValue.toString()
    }

    override fun write(asciidoc: Asciidoc) {
        asciidoc.append("|")
        asciidoc.append("[[" + getAnchor() + "]]")
        asciidoc.appendln("xref:#" + getAnchor() + "[`+", this.property.getDisplayName(), "+`]")
        writeDescription(asciidoc)
        writeDefaultValue(asciidoc)
    }

    private fun writeDescription(builder: Asciidoc) {
        builder.append("|")
        if (this.property.isDeprecated()) {
            val deprecation = this.property.getDeprecation()
            val replacement = if (deprecation != null) deprecation.replacement else null
            val reason = if (deprecation != null) deprecation.reason else null
            if (replacement != null && !replacement.isEmpty()) {
                val xref = if (this.snippets != null) this.snippets.findXref(deprecation!!.replacement) else null
                if (xref != null) {
                    builder.append("Replaced by xref:" + xref + "[`+" + deprecation!!.replacement + "+`]")
                } else {
                    builder.append("Replaced by `+" + deprecation!!.replacement + "+`")
                }
            } else if (reason != null && !reason.isEmpty()) {
                builder.append("+++", clean(reason), "+++")
            }
        } else {
            val description = this.property.getDescription()
            if (description != null && !description.isEmpty()) {
                builder.append("+++", clean(description), "+++")
            }
        }
        builder.appendln()
    }

    private fun clean(text: String): String {
        return text.replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;")
    }

    private fun writeDefaultValue(builder: Asciidoc) {
        var defaultValue = if (this.defaultValue != null) this.defaultValue else ""
        if (defaultValue.isEmpty()) {
            builder.appendln("|")
        } else {
            defaultValue = defaultValue.replace("\\", "\\\\").replace("|", "\\|")
            builder.appendln("|`+", defaultValue, "+`")
        }
    }
}
