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

plugins {
    `java-library`
    id("org.springframework.boot.conventions")
    id("org.jetbrains.kotlin.jvm")
    id("org.springframework.boot.auto-configuration")
    id("org.springframework.boot.configuration-properties")
    id("org.springframework.boot.deployed")
    id("org.springframework.boot.optional-dependencies")
}

description = "Spring Boot SendGrid"

dependencies {
    api(project(":core:spring-boot"))
    api("com.sendgrid:sendgrid-java")

    // `optional` is a custom configuration from the optional-dependencies plugin;
    // string-invoke it since Kotlin-DSL type-safe accessors aren't generated for
    // configurations contributed by a transitively-applied convention plugin.
    "optional"(project(":core:spring-boot-autoconfigure"))

    testImplementation(project(":core:spring-boot-test"))
    testImplementation(project(":test-support:spring-boot-test-support"))

    testRuntimeOnly("ch.qos.logback:logback-classic")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect")
}

// The original Groovy script set `options.nullability.checking = "tests"` on
// compileTestJava. This module's main and test sources are now Kotlin, so
// compileTestJava has no sources and the Java nullability check is a no-op;
// omitted in the Kotlin-DSL conversion.
