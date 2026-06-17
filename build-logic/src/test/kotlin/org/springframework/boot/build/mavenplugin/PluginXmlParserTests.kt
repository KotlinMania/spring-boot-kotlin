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
package org.springframework.boot.build.mavenplugin

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.boot.build.mavenplugin.PluginXmlParser.Plugin
import java.io.File
import java.io.FileNotFoundException

/**
 * Tests for [PluginXmlParser].
 * 
 * @author Andy Wilkinson
 * @author Mike Smithson
 */
internal class PluginXmlParserTests {
    private val parser: PluginXmlParser = PluginXmlParser()

    @Test
    fun parseExistingDescriptorReturnPluginDescriptor() {
        val plugin: Plugin = this.parser.parse(File("src/test/resources/plugin.xml"))
        assertThat(plugin.getGroupId()).isEqualTo("org.springframework.boot")
        assertThat(plugin.getArtifactId()).isEqualTo("spring-boot-maven-plugin")
        assertThat(plugin.getVersion()).isEqualTo("2.2.0.GRADLE-SNAPSHOT")
        assertThat(plugin.getGoalPrefix()).isEqualTo("spring-boot")
        assertThat(plugin.getMojos().stream().map(PluginXmlParser.Mojo::getGoal)).containsExactly(
            "build-info", "help",
            "repackage", "run", "start", "stop"
        )
    }

    @Test
    fun parseNonExistingFileThrowException() {
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy({ this.parser.parse(File("src/test/resources/nonexistent.xml")) })
            .withCauseInstanceOf(FileNotFoundException::class.java)
    }
}
