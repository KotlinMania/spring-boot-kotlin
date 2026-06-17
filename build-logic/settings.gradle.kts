/*
 * build-logic — included build providing spring-boot-kotlin's ported
 * Spring Boot Gradle convention plugins (org.springframework.boot.*).
 *
 * Structured after kotlinmania/km-io's build-logic composite build.
 */

pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

rootProject.name = "build-logic"
