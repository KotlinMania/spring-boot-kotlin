/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.boot.build.autoconfigure

import org.gradle.kotlin.dsl.*

import org.springframework.asm.*
import java.io.*

/**
 * An `@AutoConfiguration` class.
 * 
 * @param name name of the auto-configuration class
 * @param before values of the `before` attribute
 * @param beforeName values of the `beforeName` attribute
 * @param after values of the `after` attribute
 * @param afterName values of the `afterName` attribute
 * @author Andy Wilkinson
 */
@JvmRecord
data class AutoConfigurationClass(
    val name: String,
    val before: List<String>,
    val beforeName: List<String>,
    val after: List<String>,
    val afterName: List<String>
) {
    constructor(name: String, attributes: Map<String, List<String>>) : this(
        name, attributes.getOrDefault("before", emptyList()),
        attributes.getOrDefault("beforeName", emptyList()),
        attributes.getOrDefault("after", emptyList()),
        attributes.getOrDefault("afterName", emptyList())
    )

    class AutoConfigurationClassVisitor : ClassVisitor(SpringAsmInfo.ASM_VERSION) {
        var autoConfigurationClass: AutoConfigurationClass? = null

        var name: String? = null

        override fun visit(
            version: Int, access: Int, name: String, signature: String?, superName: String?,
            interfaces: Array<String?>?
        ) {
            this.name = Type.getObjectType(name).getClassName()
        }

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            val annotationClassName = Type.getType(descriptor).getClassName()
            if ("org.springframework.boot.autoconfigure.AutoConfiguration" == annotationClassName) {
                return AutoConfigurationAnnotationVisitor()
            }
            return null
        }

        private inner class AutoConfigurationAnnotationVisitor : AnnotationVisitor(SpringAsmInfo.ASM_VERSION) {
            val attributes = mutableMapOf<String, MutableList<String>>()

            private val INTERESTING_ATTRIBUTES = setOf("before", "beforeName", "after", "afterName")

            override fun visitEnd() {
                this@AutoConfigurationClassVisitor.autoConfigurationClass = AutoConfigurationClass(
                    this@AutoConfigurationClassVisitor.name ?: "", this.attributes
                )
            }

            override fun visitArray(attributeName: String?): AnnotationVisitor? {
                if (attributeName != null && INTERESTING_ATTRIBUTES.contains(attributeName)) {
                    return object : AnnotationVisitor(SpringAsmInfo.ASM_VERSION) {
                        override fun visit(name: String?, value: Any?) {
                            val resolved = if (value is Type) value.getClassName() else value
                            this@AutoConfigurationAnnotationVisitor.attributes
                                .getOrPut(attributeName) { mutableListOf() }
                                .add(resolved.toString())
                        }
                    }
                }
                return null
            }
        }
    }

    companion object {
        fun of(input: InputStream): AutoConfigurationClass? {
            try {
                val classReader = ClassReader(input)
                val visitor = AutoConfigurationClassVisitor()
                classReader.accept(visitor, ClassReader.SKIP_DEBUG or ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
                return visitor.autoConfigurationClass
            } catch (ex: IOException) {
                throw UncheckedIOException(ex)
            }
        }

        fun of(classFile: File): AutoConfigurationClass? {
            try {
                FileInputStream(classFile).use { input ->
                    return of(input)
                }
            } catch (ex: IOException) {
                throw UncheckedIOException(ex)
            }
        }
    }
}
