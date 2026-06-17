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

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.build.DeployedPlugin
import org.springframework.boot.build.assertj.NodeAssert
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.function.Consumer

/**
 * Tests for [BomPlugin].
 * 
 * @author Andy Wilkinson
 */
internal class BomPluginIntegrationTests {
    private var projectDir: File? = null

    private var buildFile: File? = null

    @BeforeEach
    fun setup(@TempDir projectDir: File?) {
        this.projectDir = projectDir
        this.buildFile = File(this.projectDir, "build.gradle")
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun libraryModulesAreIncludedInDependencyManagementOfGeneratedPom() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'org.springframework.boot.bom'")
            out.println("    id 'org.springframework.boot.deployed'")
            out.println("}")
            out.println("bom {")
            out.println("    library('ActiveMQ', '5.15.10') {")
            out.println("        group('org.apache.activemq') {")
            out.println("            modules = [")
            out.println("                'activemq-amqp',")
            out.println("                'activemq-blueprint'")
            out.println("            ]")
            out.println("        }")
            out.println("    }")
            out.println("}")
        }
        generatePom(Consumer { pom ->
            assertThat(pom).textAtPath("//properties/activemq.version").isEqualTo("5.15.10")
            var dependency: NodeAssert? = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[1]")
            assertThat(dependency).textAtPath("groupId").isEqualTo("org.apache.activemq")
            assertThat(dependency).textAtPath("artifactId").isEqualTo("activemq-amqp")
            assertThat(dependency).textAtPath("version").isEqualTo("\${activemq.version}")
            assertThat(dependency).textAtPath("scope").isNullOrEmpty()
            assertThat(dependency).textAtPath("type").isNullOrEmpty()
            assertThat(dependency).textAtPath("classifier").isNullOrEmpty()
            dependency = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[2]")
            assertThat(dependency).textAtPath("groupId").isEqualTo("org.apache.activemq")
            assertThat(dependency).textAtPath("artifactId").isEqualTo("activemq-blueprint")
            assertThat(dependency).textAtPath("version").isEqualTo("\${activemq.version}")
            assertThat(dependency).textAtPath("scope").isNullOrEmpty()
            assertThat(dependency).textAtPath("type").isNullOrEmpty()
            assertThat(dependency).textAtPath("classifier").isNullOrEmpty()
        })
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun libraryPluginsAreIncludedInPluginManagementOfGeneratedPom() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'org.springframework.boot.bom'")
            out.println("    id 'org.springframework.boot.deployed'")
            out.println("}")
            out.println("bom {")
            out.println("    library('Flyway', '6.0.8') {")
            out.println("        group('org.flywaydb') {")
            out.println("            plugins = [")
            out.println("                'flyway-maven-plugin'")
            out.println("            ]")
            out.println("        }")
            out.println("    }")
            out.println("}")
        }
        generatePom(Consumer { pom ->
            assertThat(pom).textAtPath("//properties/flyway.version").isEqualTo("6.0.8")
            val plugin: NodeAssert? = pom.nodeAtPath("//pluginManagement/plugins/plugin")
            assertThat(plugin).textAtPath("groupId").isEqualTo("org.flywaydb")
            assertThat(plugin).textAtPath("artifactId").isEqualTo("flyway-maven-plugin")
            assertThat(plugin).textAtPath("version").isEqualTo("\${flyway.version}")
            assertThat(plugin).textAtPath("scope").isNullOrEmpty()
            assertThat(plugin).textAtPath("type").isNullOrEmpty()
        })
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun libraryImportsAreIncludedInDependencyManagementOfGeneratedPom() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'org.springframework.boot.bom'")
            out.println("    id 'org.springframework.boot.deployed'")
            out.println("}")
            out.println("bom {")
            out.println("    library('Jackson Bom', '2.10.0') {")
            out.println("        group('com.fasterxml.jackson') {")
            out.println("            bom('jackson-bom')")
            out.println("        }")
            out.println("    }")
            out.println("}")
        }
        generatePom(Consumer { pom ->
            assertThat(pom).textAtPath("//properties/jackson-bom.version").isEqualTo("2.10.0")
            val dependency: NodeAssert? = pom.nodeAtPath("//dependencyManagement/dependencies/dependency")
            assertThat(dependency).textAtPath("groupId").isEqualTo("com.fasterxml.jackson")
            assertThat(dependency).textAtPath("artifactId").isEqualTo("jackson-bom")
            assertThat(dependency).textAtPath("version").isEqualTo("\${jackson-bom.version}")
            assertThat(dependency).textAtPath("scope").isEqualTo("import")
            assertThat(dependency).textAtPath("type").isEqualTo("pom")
            assertThat(dependency).textAtPath("classifier").isNullOrEmpty()
        })
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun moduleExclusionsAreIncludedInDependencyManagementOfGeneratedPom() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'org.springframework.boot.bom'")
            out.println("    id 'org.springframework.boot.deployed'")
            out.println("}")
            out.println("bom {")
            out.println("    library('MySQL', '8.0.18') {")
            out.println("        group('mysql') {")
            out.println("            modules = [")
            out.println("                'mysql-connector-java' {")
            out.println("                    exclude group: 'com.google.protobuf', module: 'protobuf-java'")
            out.println("                }")
            out.println("            ]")
            out.println("        }")
            out.println("    }")
            out.println("}")
        }
        generatePom(Consumer { pom ->
            assertThat(pom).textAtPath("//properties/mysql.version").isEqualTo("8.0.18")
            val dependency: NodeAssert = pom.nodeAtPath("//dependencyManagement/dependencies/dependency")
            assertThat(dependency).textAtPath("groupId").isEqualTo("mysql")
            assertThat(dependency).textAtPath("artifactId").isEqualTo("mysql-connector-java")
            assertThat(dependency).textAtPath("version").isEqualTo("\${mysql.version}")
            assertThat(dependency).textAtPath("scope").isNullOrEmpty()
            assertThat(dependency).textAtPath("type").isNullOrEmpty()
            assertThat(dependency).textAtPath("classifier").isNullOrEmpty()
            val exclusion: NodeAssert? = dependency.nodeAtPath("exclusions/exclusion")
            assertThat(exclusion).textAtPath("groupId").isEqualTo("com.google.protobuf")
            assertThat(exclusion).textAtPath("artifactId").isEqualTo("protobuf-java")
        })
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun moduleTypesAreIncludedInDependencyManagementOfGeneratedPom() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'org.springframework.boot.bom'")
            out.println("    id 'org.springframework.boot.deployed'")
            out.println("}")
            out.println("bom {")
            out.println("    library('Elasticsearch', '7.15.2') {")
            out.println("        group('org.elasticsearch.distribution.integ-test-zip') {")
            out.println("            modules = [")
            out.println("                'elasticsearch' {")
            out.println("                    type = 'zip'")
            out.println("                }")
            out.println("            ]")
            out.println("        }")
            out.println("    }")
            out.println("}")
        }
        generatePom(Consumer { pom ->
            assertThat(pom).textAtPath("//properties/elasticsearch.version").isEqualTo("7.15.2")
            val dependency: NodeAssert? = pom.nodeAtPath("//dependencyManagement/dependencies/dependency")
            assertThat(dependency).textAtPath("groupId").isEqualTo("org.elasticsearch.distribution.integ-test-zip")
            assertThat(dependency).textAtPath("artifactId").isEqualTo("elasticsearch")
            assertThat(dependency).textAtPath("version").isEqualTo("\${elasticsearch.version}")
            assertThat(dependency).textAtPath("scope").isNullOrEmpty()
            assertThat(dependency).textAtPath("type").isEqualTo("zip")
            assertThat(dependency).textAtPath("classifier").isNullOrEmpty()
            assertThat(dependency).nodeAtPath("exclusions").isNull()
        })
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun moduleClassifiersAreIncludedInDependencyManagementOfGeneratedPom() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'org.springframework.boot.bom'")
            out.println("    id 'org.springframework.boot.deployed'")
            out.println("}")
            out.println("bom {")
            out.println("    library('Kafka', '2.7.2') {")
            out.println("        group('org.apache.kafka') {")
            out.println("            modules = [")
            out.println("                'connect-api',")
            out.println("                'generator',")
            out.println("                'generator' {")
            out.println("                    classifier = 'test'")
            out.println("                },")
            out.println("                'kafka-tools',")
            out.println("            ]")
            out.println("        }")
            out.println("    }")
            out.println("}")
        }
        generatePom(Consumer { pom ->
            assertThat(pom).textAtPath("//properties/kafka.version").isEqualTo("2.7.2")
            val connectApi: NodeAssert? = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[1]")
            assertThat(connectApi).textAtPath("groupId").isEqualTo("org.apache.kafka")
            assertThat(connectApi).textAtPath("artifactId").isEqualTo("connect-api")
            assertThat(connectApi).textAtPath("version").isEqualTo("\${kafka.version}")
            assertThat(connectApi).textAtPath("scope").isNullOrEmpty()
            assertThat(connectApi).textAtPath("type").isNullOrEmpty()
            assertThat(connectApi).textAtPath("classifier").isNullOrEmpty()
            assertThat(connectApi).nodeAtPath("exclusions").isNull()
            val generator: NodeAssert? = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[2]")
            assertThat(generator).textAtPath("groupId").isEqualTo("org.apache.kafka")
            assertThat(generator).textAtPath("artifactId").isEqualTo("generator")
            assertThat(generator).textAtPath("version").isEqualTo("\${kafka.version}")
            assertThat(generator).textAtPath("scope").isNullOrEmpty()
            assertThat(generator).textAtPath("type").isNullOrEmpty()
            assertThat(generator).textAtPath("classifier").isNullOrEmpty()
            assertThat(generator).nodeAtPath("exclusions").isNull()
            val generatorTest: NodeAssert? = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[3]")
            assertThat(generatorTest).textAtPath("groupId").isEqualTo("org.apache.kafka")
            assertThat(generatorTest).textAtPath("artifactId").isEqualTo("generator")
            assertThat(generatorTest).textAtPath("version").isEqualTo("\${kafka.version}")
            assertThat(generatorTest).textAtPath("scope").isNullOrEmpty()
            assertThat(generatorTest).textAtPath("type").isNullOrEmpty()
            assertThat(generatorTest).textAtPath("classifier").isEqualTo("test")
            assertThat(generatorTest).nodeAtPath("exclusions").isNull()
            val kafkaTools: NodeAssert? = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[4]")
            assertThat(kafkaTools).textAtPath("groupId").isEqualTo("org.apache.kafka")
            assertThat(kafkaTools).textAtPath("artifactId").isEqualTo("kafka-tools")
            assertThat(kafkaTools).textAtPath("version").isEqualTo("\${kafka.version}")
            assertThat(kafkaTools).textAtPath("scope").isNullOrEmpty()
            assertThat(kafkaTools).textAtPath("type").isNullOrEmpty()
            assertThat(kafkaTools).textAtPath("classifier").isNullOrEmpty()
            assertThat(kafkaTools).nodeAtPath("exclusions").isNull()
        })
    }

    @Test
    @kotlin.Throws(IOException::class)
    fun libraryNamedSpringBootHasNoVersionProperty() {
        PrintWriter(FileWriter(this.buildFile)).use { out ->
            out.println("plugins {")
            out.println("    id 'org.springframework.boot.bom'")
            out.println("    id 'org.springframework.boot.deployed'")
            out.println("}")
            out.println("bom {")
            out.println("    library('Spring Boot', '1.2.3') {")
            out.println("        group('org.springframework.boot') {")
            out.println("            modules = [")
            out.println("                'spring-boot'")
            out.println("            ]")
            out.println("        }")
            out.println("    }")
            out.println("}")
        }
        generatePom(Consumer { pom ->
            assertThat(pom).textAtPath("//properties/spring-boot.version").isEmpty()
            val dependency: NodeAssert? = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[1]")
            assertThat(dependency).textAtPath("groupId").isEqualTo("org.springframework.boot")
            assertThat(dependency).textAtPath("artifactId").isEqualTo("spring-boot")
            assertThat(dependency).textAtPath("version").isEqualTo("1.2.3")
            assertThat(dependency).textAtPath("scope").isNullOrEmpty()
            assertThat(dependency).textAtPath("type").isNullOrEmpty()
        })
    }

    private fun runGradle(vararg args: String?): BuildResult {
        return GradleRunner.create()
            .withDebug(true)
            .withProjectDir(this.projectDir)
            .withArguments(args)
            .withPluginClasspath()
            .build()
    }

    private fun generatePom(consumer: Consumer<NodeAssert?>) {
        runGradle(DeployedPlugin.GENERATE_POM_TASK_NAME, "-s")
        val generatedPomXml: File = File(this.projectDir, "build/publications/maven/pom-default.xml")
        assertThat(generatedPomXml).isFile()
        consumer.accept(NodeAssert(generatedPomXml))
    }
}
