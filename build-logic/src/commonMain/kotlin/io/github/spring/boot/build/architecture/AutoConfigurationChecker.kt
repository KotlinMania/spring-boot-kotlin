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
package org.springframework.boot.build.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.lang.EvaluationResult
import java.util.function.Predicate

/**
 * Finds all configurations from auto-configurations (either nested configurations or
 * imported ones) and checks that these classes don't contain public members.
 * 
 * 
 * Kotlin classes are ignored as Kotlin does not have package-private visibility and
 * `internal` isn't a good substitute.
 * 
 * @author Moritz Halbritter
 */
class AutoConfigurationChecker {
    private val isAutoConfiguration: DescribedPredicate<JavaClass?> = ArchitectureRules.areRegularAutoConfiguration()
        .and(ArchitectureRules.areNotKotlinClasses())

    fun check(javaClasses: JavaClasses): EvaluationResult? {
        val autoConfigurations = AutoConfigurations(javaClasses)
        for (javaClass in javaClasses) {
            if (isAutoConfigurationOrEnclosedInAutoConfiguration(javaClass)) {
                autoConfigurations.add(javaClass)
            }
        }
        return ArchitectureRules.shouldHaveNoPublicMembers().evaluate(autoConfigurations.configurations)
    }

    private fun isAutoConfigurationOrEnclosedInAutoConfiguration(javaClass: JavaClass): Boolean {
        if (this.isAutoConfiguration.test(javaClass)) {
            return true
        }
        var enclosingClass = javaClass.getEnclosingClass().orElse(null)
        while (enclosingClass != null) {
            if (this.isAutoConfiguration.test(enclosingClass)) {
                return true
            }
            enclosingClass = enclosingClass.getEnclosingClass().orElse(null)
        }
        return false
    }

    private class AutoConfigurations(private val classes: JavaClasses) {
        private val autoConfigurationClasses: MutableMap<String?, JavaClass?> = HashMap<String?, JavaClass?>()

        fun add(autoConfiguration: JavaClass) {
            if (!autoConfiguration.isMetaAnnotatedWith(CONFIGURATION)) {
                return
            }
            if (this.autoConfigurationClasses.putIfAbsent(autoConfiguration.name, autoConfiguration) != null) {
                return
            }
            processImports(autoConfiguration, this.autoConfigurationClasses)
        }

        val configurations: JavaClasses
            get() {
                val isAutoConfiguration =
                    DescribedPredicate.describe<JavaClass?>(
                        "is an auto-configuration",
                        Predicate { c: JavaClass? ->
                            this.autoConfigurationClasses.containsKey(
                                c!!.name
                            )
                        })
                return this.classes.that(isAutoConfiguration)
            }

        fun processImports(javaClass: JavaClass, result: MutableMap<String?, JavaClass?>) {
            val importedClasses = getImportedClasses(javaClass)
            for (importedClass in importedClasses) {
                if (!isBootClass(importedClass)) {
                    continue
                }
                if (result.putIfAbsent(importedClass.name, importedClass) != null) {
                    continue
                }
                // Recursively find other imported classes
                processImports(importedClass, result)
            }
        }

        fun getImportedClasses(javaClass: JavaClass): Array<JavaClass> {
            if (!javaClass.isAnnotatedWith(IMPORT)) {
                return arrayOfNulls<JavaClass>(0)
            }
            val imports = javaClass.getAnnotationOfType(IMPORT)
            return imports.get("value").orElse(arrayOfNulls<JavaClass>(0)) as Array<JavaClass>
        }

        fun isBootClass(javaClass: JavaClass): Boolean {
            val pkg = javaClass.getPackage().name
            return pkg == SPRING_BOOT_ROOT_PACKAGE || pkg.startsWith(SPRING_BOOT_ROOT_PACKAGE + ".")
        }

        companion object {
            private const val SPRING_BOOT_ROOT_PACKAGE = "org.springframework.boot"

            private const val IMPORT = "org.springframework.context.annotation.Import"

            private const val CONFIGURATION = "org.springframework.context.annotation.Configuration"
        }
    }
}
