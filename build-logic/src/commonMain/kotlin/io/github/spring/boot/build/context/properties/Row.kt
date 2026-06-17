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

import org.gradle.kotlin.dsl.*

/**
 * Abstract class for rows in [Table].
 * 
 * @author Brian Clozel
 * @author Phillip Webb
 */
abstract class Row protected constructor(private val snippet: Snippet, private val id: String) :
    Comparable<Row?> {
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null || javaClass != obj.javaClass) {
            return false
        }
        val other = obj as Row
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }

    override fun compareTo(other: Row): Int {
        return this.id.compareTo(other.id)
    }

    val anchor: String
        get() = this.snippet.anchor + "." + this.id

    abstract fun write(asciidoc: Asciidoc?)
}
