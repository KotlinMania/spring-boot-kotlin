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
package org.springframework.boot.build.test.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.build.test.autoconfigure.TestSliceMetadata.TestSlice
import java.io.File

/**
 * Tests for [TestSliceMetadata].
 * 
 * @author Andy Wilkinson
 */
class TestSliceMetadataTests {
    @TempDir
    private val temp: File? = null

    @Test
    fun roundtripJson() {
        val source: TestSliceMetadata = TestSliceMetadata(
            "example",
            List.of(
                TestSlice("ExampleOneTest", List.of("com.example.OneAutoConfiguration")),
                TestSlice("ExampleTwoTest", List.of("com.example.TwoAutoConfiguration"))
            )
        )
        val metadataFile: File = File(this.temp, "metadata.json")
        source.writeTo(metadataFile)
        val readBack: TestSliceMetadata? = TestSliceMetadata.readFrom(metadataFile)
        assertThat(source).isEqualTo(readBack)
    }
}
