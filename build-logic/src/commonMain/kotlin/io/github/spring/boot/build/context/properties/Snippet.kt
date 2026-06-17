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

import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * A configuration properties snippet.
 * 
 * @author Brian Clozed
 * @author Phillip Webb
 */
class Snippet(anchor: String?, title: String?, config: Consumer<Config?>?) {
    val anchor: String?

    val title: String?

    val prefixes: MutableSet<String?>

    val overrides: MutableMap<String?, String?>

    init {
        val prefixes: MutableSet<String?> = LinkedHashSet<String?>()
        val overrides: MutableMap<String?, String?> = LinkedHashMap<String?, String?>()
        if (config != null) {
            config.accept(object : Config {
                override fun accept(prefix: String?) {
                    prefixes.add(prefix)
                }

                override fun accept(prefix: String?, description: String?) {
                    overrides.put(prefix, description)
                }
            })
        }
        this.anchor = anchor
        this.title = title
        this.prefixes = prefixes
        this.overrides = overrides
    }

    fun forEachPrefix(action: Consumer<String?>?) {
        this.prefixes.forEach(action)
    }

    fun forEachOverride(action: BiConsumer<String?, String?>?) {
        this.overrides.forEach(action!!)
    }

    /**
     * Callback to configure the snippet.
     */
    interface Config {
        /**
         * Accept the given prefix using the meta-data description.
         * @param prefix the prefix to accept
         */
        fun accept(prefix: String?)

        /**
         * Accept the given prefix with a defined description.
         * @param prefix the prefix to accept
         * @param description the description to use
         */
        fun accept(prefix: String?, description: String?)
    }
}
