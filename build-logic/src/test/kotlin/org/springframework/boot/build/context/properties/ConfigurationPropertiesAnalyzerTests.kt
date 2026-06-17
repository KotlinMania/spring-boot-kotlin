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
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.build.context.properties.ConfigurationPropertiesAnalyzer.Analysis
import org.springframework.boot.build.context.properties.ConfigurationPropertiesAnalyzer.Report
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Collections

/**
 * Tests for [ConfigurationPropertiesAnalyzer].
 * 
 * @author Stephane Nicoll
 */
internal class ConfigurationPropertiesAnalyzerTests {
    @Test
    fun createAnalyzerWithNoSource() {
        assertThatIllegalArgumentException()
            .isThrownBy({ ConfigurationPropertiesAnalyzer(Collections.emptyList()) })
            .withMessage("At least one source should be provided")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun analyzeOrderWithAlphabeticalOrder(@TempDir tempDir: File?) {
        val metadata: File = File(tempDir, "metadata.json")
        Files.writeString(
            metadata.toPath(), """
				{ "properties": [
					{ "name": "abc"}, {"name": "def"}, {"name": "xyz"}
				  ]
				}
				""".trimIndent()
        )
        val report: Report = Report(tempDir)
        val analyzer: ConfigurationPropertiesAnalyzer = ConfigurationPropertiesAnalyzer(List.of(metadata))
        analyzer.analyzeOrder(report)
        assertThat(report.hasProblems()).isFalse()
        assertThat(report.getAnalyses(metadata)).singleElement()
            .satisfies(({ analysis -> assertThat(analysis.getItems()).isEmpty() }))
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun analyzeOrderWithViolations(@TempDir tempDir: File?) {
        val metadata: File = File(tempDir, "metadata.json")
        Files.writeString(
            metadata.toPath(), """
				{ "properties": [
					{ "name": "def"}, {"name": "abc"}, {"name": "xyz"}
				  ]
				}
				""".trimIndent()
        )
        val report: Report = Report(tempDir)
        val analyzer: ConfigurationPropertiesAnalyzer = ConfigurationPropertiesAnalyzer(List.of(metadata))
        analyzer.analyzeOrder(report)
        assertThat(report.hasProblems()).isTrue()
        assertThat(report.getAnalyses(metadata)).singleElement()
            .satisfies({ analysis ->
                assertThat(analysis.getItems()).containsExactly(
                    "Wrong order at $.properties[0].name - expected 'abc' but found 'def'",
                    "Wrong order at $.properties[1].name - expected 'def' but found 'abc'"
                )
            })
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun analyzeDuplicatesWithNoDuplicates(@TempDir tempDir: File?) {
        val metadata: File = File(tempDir, "metadata.json")
        Files.writeString(
            metadata.toPath(), """
				{ "properties": [
					{ "name": "abc"}, {"name": "def"}, {"name": "xyz"}
				  ]
				}
				""".trimIndent()
        )
        val report: Report = Report(tempDir)
        val analyzer: ConfigurationPropertiesAnalyzer = ConfigurationPropertiesAnalyzer(List.of(metadata))
        analyzer.analyzeOrder(report)
        assertThat(report.hasProblems()).isFalse()
        assertThat(report.getAnalyses(metadata)).singleElement()
            .satisfies(({ analysis -> assertThat(analysis.getItems()).isEmpty() }))
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun analyzeDuplicatesWithDuplicate(@TempDir tempDir: File?) {
        val metadata: File = File(tempDir, "metadata.json")
        Files.writeString(
            metadata.toPath(), """
				{ "properties": [
					{ "name": "abc"}, {"name": "abc"}, {"name": "def"}
				  ]
				}
				""".trimIndent()
        )
        val report: Report = Report(tempDir)
        val analyzer: ConfigurationPropertiesAnalyzer = ConfigurationPropertiesAnalyzer(List.of(metadata))
        analyzer.analyzeDuplicates(report)
        assertThat(report.hasProblems()).isTrue()
        assertThat(report.getAnalyses(metadata)).singleElement()
            .satisfies({ analysis ->
                assertThat(analysis.getItems())
                    .containsExactly("Duplicate name 'abc' at $.properties[1]")
            })
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun analyzePropertyDescription(@TempDir tempDir: File?) {
        val metadata: File = File(tempDir, "metadata.json")
        Files.writeString(
            metadata.toPath(), """
				{ "properties": [
					{ "name": "abc", "description": "This is abc." },
					{ "name": "def", "description": "This is def." },
					{ "name": "xyz", "description": "This is xyz." }
				  ]
				}
				""".trimIndent()
        )
        val report: Report = Report(tempDir)
        val analyzer: ConfigurationPropertiesAnalyzer = ConfigurationPropertiesAnalyzer(List.of(metadata))
        analyzer.analyzePropertyDescription(report, List.of())
        assertThat(report.hasProblems()).isFalse()
        assertThat(report.getAnalyses(metadata)).singleElement()
            .satisfies(({ analysis -> assertThat(analysis.getItems()).isEmpty() }))
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun analyzePropertyDescriptionWithMissingDescription(@TempDir tempDir: File?) {
        val metadata: File = File(tempDir, "metadata.json")
        Files.writeString(
            metadata.toPath(), """
				{ "properties": [
					{ "name": "abc", "description": "This is abc." },
					{ "name": "def" },
					{ "name": "xyz", "description": "This is xyz." }
				  ]
				}
				""".trimIndent()
        )
        val report: Report = Report(tempDir)
        val analyzer: ConfigurationPropertiesAnalyzer = ConfigurationPropertiesAnalyzer(List.of(metadata))
        analyzer.analyzePropertyDescription(report, List.of())
        assertThat(report.hasProblems()).isTrue()
        assertThat(report.getAnalyses(metadata)).singleElement()
            .satisfies(({ analysis -> assertThat(analysis.getItems()).containsExactly("def") }))
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun analyzeDeprecatedPropertyWithMissingSince(@TempDir tempDir: File?) {
        val metadata: File = File(tempDir, "metadata.json")
        Files.writeString(
            metadata.toPath(), """
				{ "properties": [
				  {
				    "name": "abc",
				    "description": "This is abc.",
				    "deprecation": { "reason": "abc reason", "since": "3.0.0" }
				  },
				  { "name": "def", "description": "This is def." },
				  {
				    "name": "xyz",
				    "description": "This is xyz.",
				    "deprecation": { "reason": "xyz reason" }
				  }
				  ]
				}
				""".trimIndent()
        )
        val report: Report = Report(tempDir)
        val analyzer: ConfigurationPropertiesAnalyzer = ConfigurationPropertiesAnalyzer(List.of(metadata))
        analyzer.analyzeDeprecationSince(report)
        assertThat(report.hasProblems()).isTrue()
        assertThat(report.getAnalyses(metadata)).singleElement()
            .satisfies(({ analysis -> assertThat(analysis.getItems()).containsExactly("xyz") }))
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun writeEmptyReport(@TempDir tempDir: File?) {
        assertThat(writeToFile(tempDir, Report(tempDir))).hasContent("No problems found.")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun writeReportWithNoProblemsFound(@TempDir tempDir: File?) {
        val report: Report = Report(tempDir)
        val first: File = File(tempDir, "metadata-1.json")
        report.registerAnalysis(first, Analysis("Check for things:"))
        val second: File = File(tempDir, "metadata-2.json")
        report.registerAnalysis(second, Analysis("Check for other things:"))
        assertThat(writeToFile(tempDir, report)).content().isEqualToIgnoringNewLines(
            """
				metadata-1.json
				No problems found.

				metadata-2.json
				No problems found.
				
				""".trimIndent()
        )
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun writeReportWithOneProblem(@TempDir tempDir: File?) {
        val report: Report = Report(tempDir)
        val metadata: File = File(tempDir, "metadata-1.json")
        val analysis: Analysis = Analysis("Check for things:")
        analysis.addItem("Should not be deprecated")
        report.registerAnalysis(metadata, analysis)
        report.registerAnalysis(metadata, Analysis("Check for other things:"))
        assertThat(writeToFile(tempDir, report)).content().isEqualToIgnoringNewLines(
            """
				metadata-1.json
				Check for things:
					- Should not be deprecated

				Check for other things:
				No problems found.
				
				""".trimIndent()
        )
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun writeReportWithSeveralProblems(@TempDir tempDir: File?) {
        val report: Report = Report(tempDir)
        val metadata: File = File(tempDir, "metadata-1.json")
        val firstAnalysis: Analysis = Analysis("Check for things:")
        firstAnalysis.addItem("Should not be deprecated")
        firstAnalysis.addItem("Should not be public")
        report.registerAnalysis(metadata, firstAnalysis)
        val secondAnalysis: Analysis = Analysis("Check for other things:")
        secondAnalysis.addItem("Field 'this' not expected")
        report.registerAnalysis(metadata, secondAnalysis)
        assertThat(writeToFile(tempDir, report)).content().isEqualToIgnoringNewLines(
            """
				metadata-1.json
				Check for things:
					- Should not be deprecated
					- Should not be public

				Check for other things:
					- Field 'this' not expected
				
				""".trimIndent()
        )
    }

    @kotlin.Throws(IOException::class)
    private fun writeToFile(directory: File?, report: Report): File {
        val file: File = File(directory, "report.txt")
        report.write(file)
        return file
    }
}
