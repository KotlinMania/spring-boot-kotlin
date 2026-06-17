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
 * Tests for [CompoundRow].
 * 
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
internal class CompoundRowTests {
    @Test
    fun simpleProperty() {
        val row: CompoundRow = CompoundRow(
            org.springframework.boot.build.context.properties.CompoundRowTests.Companion.SNIPPET,
            "spring.test",
            "This is a description."
        )
        row.addProperty(ConfigurationProperty("spring.test.first", "java.lang.String"))
        row.addProperty(ConfigurationProperty("spring.test.second", "java.lang.String"))
        row.addProperty(ConfigurationProperty("spring.test.third", "java.lang.String"))
        val asciidoc: Asciidoc = Asciidoc()
        row.write(asciidoc)
        assertThat(asciidoc).hasToString(
            ("|[[my.spring.test]]xref:#my.spring.test[`+spring.test.first+` +" + org.springframework.boot.build.context.properties.CompoundRowTests.Companion.NEWLINE
                    + "`+spring.test.second+` +" + org.springframework.boot.build.context.properties.CompoundRowTests.Companion.NEWLINE + "`+spring.test.third+` +" + org.springframework.boot.build.context.properties.CompoundRowTests.Companion.NEWLINE + "]" + org.springframework.boot.build.context.properties.CompoundRowTests.Companion.NEWLINE
                    + "|+++This is a description.+++" + org.springframework.boot.build.context.properties.CompoundRowTests.Companion.NEWLINE + "|" + org.springframework.boot.build.context.properties.CompoundRowTests.Companion.NEWLINE)
        )
    }

    companion object {
        private val NEWLINE: String? = System.lineSeparator()

        private val SNIPPET: Snippet = Snippet("my", "title", null)
    }
}
