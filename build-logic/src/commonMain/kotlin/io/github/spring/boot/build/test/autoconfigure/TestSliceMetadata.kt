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

import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import java.io.File

/**
 * Metadata describing a module's test slices.
 * 
 * @param module the module's name
 * @param testSlices the module's test slices
 * @author Andy Wilkinson
 */
@JvmRecord
data class TestSliceMetadata(val module: String?, val testSlices: MutableList<TestSlice?>?) {
    fun writeTo(file: File?) {
        JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build().writeValue(file, this)
    }

    @JvmRecord
    data class TestSlice(val annotation: String?, val importedAutoConfigurations: MutableList<String?>?)

    companion object {
        fun readFrom(file: File?): TestSliceMetadata? {
            return JsonMapper.builder().build().readValue<TestSliceMetadata?>(file, TestSliceMetadata::class.java)
        }
    }
}
