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
 * Tests for [SingleRow].
 * 
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
internal class SingleRowTests {
    @Test
    fun simpleProperty() {
        val property: ConfigurationProperty = ConfigurationProperty(
            "spring.test.prop", "java.lang.String", "something",
            "This is a description.", false, null
        )
        val row: SingleRow =
            SingleRow(org.springframework.boot.build.context.properties.SingleRowTests.Companion.SNIPPET, property)
        val asciidoc: Asciidoc = Asciidoc()
        row.write(asciidoc)
        assertThat(asciidoc).hasToString(
            ("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
                    + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|+++This is a description.+++" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|`+something+`" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE)
        )
    }

    @Test
    fun noDefaultValue() {
        val property: ConfigurationProperty = ConfigurationProperty(
            "spring.test.prop", "java.lang.String", null,
            "This is a description.", false, null
        )
        val row: SingleRow =
            SingleRow(org.springframework.boot.build.context.properties.SingleRowTests.Companion.SNIPPET, property)
        val asciidoc: Asciidoc = Asciidoc()
        row.write(asciidoc)
        assertThat(asciidoc).hasToString(
            ("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
                    + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|+++This is a description.+++" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE)
        )
    }

    @Test
    fun defaultValueWithPipes() {
        val property: ConfigurationProperty = ConfigurationProperty(
            "spring.test.prop", "java.lang.String",
            "first|second", "This is a description.", false, null
        )
        val row: SingleRow =
            SingleRow(org.springframework.boot.build.context.properties.SingleRowTests.Companion.SNIPPET, property)
        val asciidoc: Asciidoc = Asciidoc()
        row.write(asciidoc)
        assertThat(asciidoc).hasToString(
            ("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
                    + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|+++This is a description.+++" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|`+first\\|second+`" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE)
        )
    }

    @Test
    fun defaultValueWithBackslash() {
        val property: ConfigurationProperty = ConfigurationProperty(
            "spring.test.prop", "java.lang.String",
            "first\\second", "This is a description.", false, null
        )
        val row: SingleRow =
            SingleRow(org.springframework.boot.build.context.properties.SingleRowTests.Companion.SNIPPET, property)
        val asciidoc: Asciidoc = Asciidoc()
        row.write(asciidoc)
        assertThat(asciidoc).hasToString(
            ("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
                    + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|+++This is a description.+++" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|`+first\\\\second+`" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE)
        )
    }

    @Test
    fun descriptionWithPipe() {
        val property: ConfigurationProperty = ConfigurationProperty(
            "spring.test.prop", "java.lang.String", null,
            "This is a description with a | pipe.", false, null
        )
        val row: SingleRow =
            SingleRow(org.springframework.boot.build.context.properties.SingleRowTests.Companion.SNIPPET, property)
        val asciidoc: Asciidoc = Asciidoc()
        row.write(asciidoc)
        assertThat(asciidoc).hasToString(
            ("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
                    + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|+++This is a description with a \\| pipe.+++" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE)
        )
    }

    @Test
    fun mapProperty() {
        val property: ConfigurationProperty = ConfigurationProperty(
            "spring.test.prop",
            "java.util.Map<java.lang.String,java.lang.String>", null, "This is a description.", false, null
        )
        val row: SingleRow =
            SingleRow(org.springframework.boot.build.context.properties.SingleRowTests.Companion.SNIPPET, property)
        val asciidoc: Asciidoc = Asciidoc()
        row.write(asciidoc)
        assertThat(asciidoc).hasToString(
            ("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop.*+`]"
                    + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|+++This is a description.+++" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE)
        )
    }

    @Test
    fun listProperty() {
        val defaultValue: Array<String?> = arrayOf<String>("first", "second", "third")
        val property: ConfigurationProperty = ConfigurationProperty(
            "spring.test.prop",
            "java.util.List<java.lang.String>", defaultValue, "This is a description.", false, null
        )
        val row: SingleRow =
            SingleRow(org.springframework.boot.build.context.properties.SingleRowTests.Companion.SNIPPET, property)
        val asciidoc: Asciidoc = Asciidoc()
        row.write(asciidoc)
        assertThat(asciidoc).hasToString(
            ("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
                    + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|+++This is a description.+++" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "|`+first," + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE + "second," + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE
                    + "third+`" + org.springframework.boot.build.context.properties.SingleRowTests.Companion.NEWLINE)
        )
    }

    companion object {
        private val NEWLINE: String? = System.lineSeparator()

        private val SNIPPET: Snippet = Snippet("my", "title", null)
    }
}
