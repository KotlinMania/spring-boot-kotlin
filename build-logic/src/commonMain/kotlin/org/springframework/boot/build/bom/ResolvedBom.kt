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
package org.springframework.boot.build.bom

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.json.JsonMapper
import java.io.*
import java.net.URI
import java.util.function.UnaryOperator

/**
 * A resolved bom.
 * 
 * @author Andy Wilkinson
 * @param id the ID of the resolved bom
 * @param libraries the libraries declared in the bom
 */
@JvmRecord
data class ResolvedBom(val id: Id?, val libraries: MutableList<ResolvedLibrary?>?) {
    fun writeTo(writer: Writer?) {
        jsonMapper.writeValue(writer, this)
    }

    @JvmRecord
    data class ResolvedLibrary(
        val name: String?,
        val version: String?,
        val versionProperty: String?,
        val managedDependencies: MutableList<Id?>?,
        val importedBoms: MutableList<Bom?>?,
        val links: Links?
    )

    @JvmRecord
    data class Id(val groupId: String?, val artifactId: String?, val version: String?, val classifier: String?) :
        Comparable<Id?> {
        internal constructor(groupId: String?, artifactId: String?, version: String?) : this(
            groupId,
            artifactId,
            version,
            null
        )

        override fun compareTo(o: Id): Int {
            var result = this.groupId!!.compareTo(o.groupId!!)
            if (result != 0) {
                return result
            }
            result = this.artifactId!!.compareTo(o.artifactId!!)
            if (result != 0) {
                return result
            }
            return this.version!!.compareTo(o.version!!)
        }

        override fun toString(): String {
            val builder = StringBuilder()
            builder.append(this.groupId)
            builder.append(":")
            builder.append(this.artifactId)
            builder.append(":")
            builder.append(this.version)
            if (this.classifier != null) {
                builder.append(":")
                builder.append(this.classifier)
            }
            return builder.toString()
        }
    }

    @JvmRecord
    data class Bom(
        val id: Id?,
        val parent: Bom?,
        val managedDependencies: MutableList<Id?>?,
        val importedBoms: MutableList<Bom?>?
    )

    @JvmRecord
    data class Links(val javadoc: MutableList<JavadocLink?>?)

    @JvmRecord
    data class JavadocLink(val uri: URI?, val packages: MutableList<String?>?)

    companion object {
        private val jsonMapper: JsonMapper

        init {
            jsonMapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(UnaryOperator { value: JsonInclude.Value? ->
                    value!!.withContentInclusion(
                        JsonInclude.Include.NON_EMPTY
                    )
                })
                .build()
        }

        fun readFrom(file: File): ResolvedBom? {
            try {
                FileReader(file).use { reader ->
                    return jsonMapper.readValue<ResolvedBom?>(reader, ResolvedBom::class.java)
                }
            } catch (ex: IOException) {
                throw UncheckedIOException(ex)
            }
        }
    }
}
