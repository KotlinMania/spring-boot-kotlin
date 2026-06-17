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
package org.springframework.boot.build.bom.bomr.github

import org.springframework.web.client.RestTemplate
import java.util.*

/**
 * Minimal representation of a GitHub issue.
 * 
 * @author Andy Wilkinson
 */
class Issue internal constructor(
    private val rest: RestTemplate,
    val number: Int,
    val title: String?,
    val state: State?
) {
    /**
     * Labels the issue with the given `labels`. Any existing labels are removed.
     * @param labels the labels to apply to the issue
     */
    fun label(labels: MutableList<String?>?) {
        val body = Collections.singletonMap<String?, MutableList<String?>?>("labels", labels)
        this.rest.put("issues/" + this.number + "/labels", body)
    }

    enum class State {
        /**
         * The issue is open.
         */
        OPEN,

        /**
         * The issue is closed.
         */
        CLOSED;

        companion object {
            fun of(state: String?): State {
                if ("open" == state) {
                    return State.OPEN
                }
                if ("closed" == state) {
                    return State.CLOSED
                } else {
                    throw IllegalArgumentException("Unknown state '" + state + "'")
                }
            }
        }
    }
}
