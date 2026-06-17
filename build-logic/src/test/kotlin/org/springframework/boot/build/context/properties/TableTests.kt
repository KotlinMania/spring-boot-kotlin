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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for [Table].
 * 
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
internal class TableTests {
    @Test
    fun simpleTable() {
        val table: Table = Table()
        table.addRow(
            SingleRow(
                org.springframework.boot.build.context.properties.TableTests.Companion.SNIPPET, ConfigurationProperty(
                    "spring.test.prop", "java.lang.String",
                    "something", "This is a description.", false, null
                )
            )
        )
        table.addRow(
            SingleRow(
                org.springframework.boot.build.context.properties.TableTests.Companion.SNIPPET, ConfigurationProperty(
                    "spring.test.other", "java.lang.String",
                    "other value", "This is another description.", false, null
                )
            )
        )
        val asciidoc: Asciidoc = Asciidoc()
        table.write(asciidoc)
        // @formatter:off
        assertThat(asciidoc).hasToString("[cols=\"4,3,3\", options=\"header\"]" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + 
        "|===" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + 
        "|Name|Description|Default Value" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + 
        "|[[my.spring.test.other]]xref:#my.spring.test.other[`+spring.test.other+`]" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + 
        "|+++This is another description.+++" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + 
        "|`+other value+`" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + 
        "|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + 
        "|+++This is a description.+++" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + 
        "|`+something+`" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE + 
        "|===" + org.springframework.boot.build.context.properties.TableTests.Companion.NEWLINE)
    		// @formatter:on
    }

    companion object {
        private val NEWLINE: String? = System.lineSeparator()

        private val SNIPPET: Snippet = Snippet("my", "title", null)
    }
}
