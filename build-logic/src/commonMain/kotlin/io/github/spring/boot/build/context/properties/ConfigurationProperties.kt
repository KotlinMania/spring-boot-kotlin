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

import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.util.*
import java.util.stream.Stream

/**
 * Configuration properties read from one or more
 * `META-INF/spring-configuration-metadata.json` files.
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConfigurationProperties private constructor(properties: MutableList<ConfigurationProperty>) {
    private val byName: MutableMap<String?, ConfigurationProperty?>

    init {
        val byName: MutableMap<String?, ConfigurationProperty?> = LinkedHashMap<String?, ConfigurationProperty?>()
        for (property in properties) {
            byName.put(property.name, property)
        }
        this.byName = Collections.unmodifiableMap<String?, ConfigurationProperty?>(byName)
    }

    fun get(propertyName: String?): ConfigurationProperty? {
        return this.byName.get(propertyName)
    }

    fun stream(): Stream<ConfigurationProperty?> {
        return this.byName.values.stream()
    }

    companion object {
        fun fromFiles(files: Iterable<File?>): ConfigurationProperties {
            val jsonMapper = JsonMapper()
            val properties: MutableList<ConfigurationProperty> = ArrayList<ConfigurationProperty>()
            for (file in files) {
                @Suppress("UNCHECKED_CAST")
                val json = jsonMapper.readValue(file, MutableMap::class.java) as MutableMap<String?, Any?>
                for (property in (json.get("properties") as kotlin.collections.MutableList<kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?>>?)!!) {
                    properties.add(ConfigurationProperty.Companion.fromJsonProperties(property))
                }
            }
            return ConfigurationProperties(properties)
        }
    }
}
