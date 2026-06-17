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
 * A configuration property.
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConfigurationProperty @JvmOverloads constructor(
    val name: String,
    val type: String?,
    val defaultValue: Any? = null,
    val description: String? = null,
    val isDeprecated: Boolean = false,
    val deprecation: Deprecation? = null
) {
    val displayName: String?
        get() = if (this.type != null && this.type.startsWith("java.util.Map")) this.name + ".*" else this.name

    override fun toString(): String {
        return "ConfigurationProperty [name=" + this.name + ", type=" + this.type + "]"
    }

    @JvmRecord
    data class Deprecation(
        val reason: String?,
        val replacement: String?,
        val since: String?,
        val level: String?
    ) {
        companion object {
            fun fromJsonProperties(property: MutableMap<String?, Any?>?): Deprecation? {
                if (property == null) {
                    return null
                }
                val reason = property.get("reason") as String?
                val replacement = property.get("replacement") as String?
                val since = property.get("since") as String?
                val level = property.get("level") as String?
                return Deprecation(reason, replacement, since, level)
            }
        }
    }

    companion object {
        fun fromJsonProperties(property: MutableMap<String?, Any?>): ConfigurationProperty {
            val name = property.get("name") as String
            val type = property.get("type") as String?
            val defaultValue = property.get("defaultValue")
            val description = property.get("description") as String?
            val deprecated = property.containsKey("deprecated")
            val deprecation = property.get("deprecation") as MutableMap<String?, Any?>?
            return ConfigurationProperty(
                name, type, defaultValue, description, deprecated,
                Deprecation.Companion.fromJsonProperties(deprecation)
            )
        }
    }
}
