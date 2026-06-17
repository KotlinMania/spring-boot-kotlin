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

import org.springframework.asm.*
import java.io.*
import java.util.*

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
    val name: String?,
    val before: MutableList<String?>?,
    val beforeName: MutableList<String?>?,
    val after: MutableList<String?>?,
    val afterName: MutableList<String?>?
) {
    constructor(name: String?, attributes: MutableMap<String?, MutableList<String?>?>) : this(
        name, attributes.getOrDefault("before", mutableListOf<String?>()),
        attributes.getOrDefault("beforeName", mutableListOf<String?>()),
        attributes.getOrDefault("after", mutableListOf<String?>()),
        attributes.getOrDefault("afterName", mutableListOf<String?>())
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
            val attributes: MutableMap<String?, MutableList<String?>?> =
                HashMap<String?, MutableList<String?>?>()

            override fun visitEnd() {
                this@AutoConfigurationClassVisitor.autoConfigurationClass = AutoConfigurationClass(
                    this@AutoConfigurationClassVisitor.name, this.attributes
                )
            }

            override fun visitArray(attributeName: String?): AnnotationVisitor? {
                if (INTERESTING_ATTRIBUTES.contains(attributeName)) {
                    return object : AnnotationVisitor(SpringAsmInfo.ASM_VERSION) {
                        override fun visit(name: String?, value: Any?) {
                            var value = value
                            if (value is Type) {
                                value = value.getClassName()
                            }
                            this@AutoConfigurationAnnotationVisitor.attributes
                                .computeIfAbsent(attributeName) { n: kotlin.String? -> java.util.ArrayList<kotlin.String?>() }!!
                                .add(Objects.toString(value))
                        }
                    }
                }
                return null
            }

            companion object {
                val INTERESTING_ATTRIBUTES = mutableSetOf<String?>(
                    "before", "beforeName", "after",
                    "afterName"
                )
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
