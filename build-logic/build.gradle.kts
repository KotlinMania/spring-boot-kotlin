/*
 * spring-boot-kotlin build-logic — Kotlin DSL conversion of Spring Boot's
 * buildSrc. Registers the org.springframework.boot.* convention plugins.
 *
 * Version values were recovered from Spring Boot's original gradle.properties
 * (git history) and inlined here; migrate to gradle/libs.versions.toml later.
 */

plugins {
    `kotlin-dsl`
    id("io.spring.javaformat") version "0.0.47"
    checkstyle
    eclipse
    id("org.jetbrains.dokka") version "2.1.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    // Required: spring-framework-bom:7.0.8-SNAPSHOT (and spring-context/core/web)
    // are snapshots, published only here.
    maven { url = uri("https://repo.spring.io/snapshot") }
}

kotlin {
    jvmToolchain(21)
    // build-logic is a JVM `kotlin-dsl` build (no KMP source sets). Point the
    // standard `main` source set at the commonMain-named dirs the repo uses.
    sourceSets.named("main") {
        kotlin.srcDir("src/commonMain/kotlin")
        resources.srcDir("src/commonMain/resources")
    }
}

checkstyle {
    toolVersion = "10.12.4"
}

dependencies {
    checkstyle("com.puppycrawl.tools:checkstyle:10.12.4")
    checkstyle("io.spring.javaformat:spring-javaformat-checkstyle:0.0.47")

    implementation(platform("com.fasterxml.jackson:jackson-bom:2.21.4"))
    implementation(platform("tools.jackson:jackson-bom:3.1.4"))
    implementation(platform("org.springframework:spring-framework-bom:7.0.8-SNAPSHOT"))
    implementation("com.github.node-gradle:gradle-node-plugin:7.1.0")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.6")
    implementation("com.gradle:develocity-gradle-plugin:4.2.2")
    implementation("commons-codec:commons-codec:1.21.0")
    implementation("de.undercouch.download:de.undercouch.download.gradle.plugin:5.5.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.1.0")
    implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.0")
    implementation("io.spring.javaformat:spring-javaformat-gradle-plugin:0.0.47")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-web")
    implementation("org.yaml:snakeyaml:2.6")
    implementation("io.spring.gradle.nullability:nullability-plugin:0.0.11")
    implementation("tools.jackson.core:jackson-databind")

    // Folded in from the former gradle/plugins included build (cycle-detection
    // settings plugin); jgrapht backs its dependency-cycle analysis.
    implementation("org.jgrapht:jgrapht-core:1.5.2")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.springframework:spring-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-api")
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "ch.qos.logback", module = "logback-core")

    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.MINUTES)
}

gradlePlugin {
    plugins {
        register("aggregatorPlugin") {
            id = "io.github.kotlinmania.spring.boot.aggregator"
            implementationClass = "org.springframework.boot.build.aggregation.AggregatorPlugin"
        }
        register("annotationProcessorPlugin") {
            id = "io.github.kotlinmania.spring.boot.annotation-processor"
            implementationClass = "org.springframework.boot.build.processors.AnnotationProcessorPlugin"
        }
        register("cycleDetectionPlugin") {
            id = "io.github.kotlinmania.spring.boot.cycle-detection"
            implementationClass = "org.springframework.boot.build.cycledetection.CycleDetectionPlugin"
        }
        register("autoConfigurationPlugin") {
            id = "io.github.kotlinmania.spring.boot.auto-configuration"
            implementationClass = "org.springframework.boot.build.autoconfigure.AutoConfigurationPlugin"
        }
        register("bomPlugin") {
            id = "io.github.kotlinmania.spring.boot.bom"
            implementationClass = "org.springframework.boot.build.bom.BomPlugin"
        }
        register("configurationMetadataPlugin") {
            id = "io.github.kotlinmania.spring.boot.configuration-metadata"
            implementationClass = "org.springframework.boot.build.context.properties.ConfigurationMetadataPlugin"
        }
        register("configurationPropertiesPlugin") {
            id = "io.github.kotlinmania.spring.boot.configuration-properties"
            implementationClass = "org.springframework.boot.build.context.properties.ConfigurationPropertiesPlugin"
        }
        register("conventionsPlugin") {
            id = "io.github.kotlinmania.spring.boot.conventions"
            implementationClass = "io.github.kotlinmania.spring.boot.build.ConventionsPlugin"
        }
        register("deployedPlugin") {
            id = "io.github.kotlinmania.spring.boot.deployed"
            implementationClass = "org.springframework.boot.build.DeployedPlugin"
        }
        register("dockerTestPlugin") {
            id = "io.github.kotlinmania.spring.boot.docker-test"
            implementationClass = "org.springframework.boot.build.test.DockerTestPlugin"
        }
        register("integrationTestPlugin") {
            id = "io.github.kotlinmania.spring.boot.integration-test"
            implementationClass = "org.springframework.boot.build.test.IntegrationTestPlugin"
        }
        register("mavenRepositoryPlugin") {
            id = "io.github.kotlinmania.spring.boot.maven-repository"
            implementationClass = "org.springframework.boot.build.MavenRepositoryPlugin"
        }
        register("optionalDependenciesPlugin") {
            id = "io.github.kotlinmania.spring.boot.optional-dependencies"
            implementationClass = "org.springframework.boot.build.optional.OptionalDependenciesPlugin"
        }
        register("starterPlugin") {
            id = "io.github.kotlinmania.spring.boot.starter"
            implementationClass = "org.springframework.boot.build.starters.StarterPlugin"
        }
        register("systemTestPlugin") {
            id = "io.github.kotlinmania.spring.boot.system-test"
            implementationClass = "org.springframework.boot.build.test.SystemTestPlugin"
        }
        register("testAutoConfigurationPlugin") {
            id = "io.github.kotlinmania.spring.boot.test-auto-configuration"
            implementationClass = "org.springframework.boot.build.test.autoconfigure.TestAutoConfigurationPlugin"
        }
        register("testFailuresPlugin") {
            id = "io.github.kotlinmania.spring.boot.test-failures"
            implementationClass = "org.springframework.boot.build.testing.TestFailuresPlugin"
        }
        register("testSlicePlugin") {
            id = "io.github.kotlinmania.spring.boot.test-slice"
            implementationClass = "org.springframework.boot.build.test.autoconfigure.TestSlicePlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

eclipse {
    jdt {
        file {
            withProperties(closureOf<java.util.Properties> {
                setProperty("org.eclipse.jdt.core.compiler.ignoreUnnamedModuleForSplitPackage", "enabled")
            })
        }
    }
}

// NOTE: Spring's buildSrc had `jar.dependsOn check`. With the `kotlin-dsl`
// plugin, test compilation depends on `jar` (compileTestKotlin -> jar), so
// forcing jar -> check creates a cycle. Dropped; `check` still runs under the
// standard `build`/`check` lifecycle.
