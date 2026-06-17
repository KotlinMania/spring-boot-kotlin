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
 * Asciidoctor table listing configuration properties sharing to a common theme.
 * 
 * @author Brian Clozel
 */
internal class Table {
    private val rows: MutableSet<Row?> = TreeSet<Row?>()

    fun addRow(row: Row?) {
        this.rows.add(row)
    }

    fun write(asciidoc: Asciidoc) {
        asciidoc.appendln("[cols=\"4,3,3\", options=\"header\"]")
        asciidoc.appendln("|===")
        asciidoc.appendln("|Name|Description|Default Value")
        asciidoc.appendln()
        this.rows.forEach(Consumer { entry: Row? ->
            entry!!.write(asciidoc)
            asciidoc.appendln()
        })
        asciidoc.appendln("|===")
    }

    val isEmpty: Boolean
        get() = this.rows.isEmpty()
}
