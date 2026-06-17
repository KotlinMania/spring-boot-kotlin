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
package org.springframework.boot.build.architecture.annotations

/**
 * `@DeprecatedConfigurationProperty` analogue for architecture checks.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestDeprecatedConfigurationProperty(
    /**
     * The reason for the deprecation.
     * @return the deprecation reason
     */
    val reason: String = "",
    /**
     * The field that should be used instead (if any).
     * @return the replacement field
     */
    val replacement: String = "",
    /**
     * The version in which the property became deprecated.
     * @return the version
     */
    val since: String = ""
)
