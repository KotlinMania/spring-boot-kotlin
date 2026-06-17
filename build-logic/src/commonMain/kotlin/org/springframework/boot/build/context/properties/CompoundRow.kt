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
import java.util.function.Consumer

/**
 * Table row regrouping a list of configuration properties sharing the same description.
 * 
 * @author Brian Clozel
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
internal class CompoundRow(snippet: Snippet?, prefix: String?, private val description: String?) :
    Row(snippet, prefix) {
    private val propertyNames: MutableSet<String?>

    init {
        this.propertyNames = TreeSet<String?>()
    }

    fun addProperty(property: ConfigurationProperty) {
        this.propertyNames.add(property.getDisplayName())
    }

    val isEmpty: Boolean
        get() = this.propertyNames.isEmpty()

    override fun write(asciidoc: Asciidoc) {
        asciidoc.append("|")
        asciidoc.append("[[" + getAnchor() + "]]")
        asciidoc.append("xref:#" + getAnchor() + "[")
        this.propertyNames.forEach(Consumer { items: String? -> asciidoc.appendWithHardLineBreaks(items) })
        asciidoc.appendln("]")
        asciidoc.appendln("|+++", this.description, "+++")
        asciidoc.appendln("|")
    }
}
