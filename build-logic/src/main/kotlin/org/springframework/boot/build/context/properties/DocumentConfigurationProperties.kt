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
package org.springframework.boot.build.context.properties

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.IOException
import java.util.function.Consumer

/**
 * [Task] used to document auto-configuration classes.
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract class DocumentConfigurationProperties : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    var configurationPropertyMetadata: FileCollection? = null

    @get:Input
    abstract val deprecated: Property<Boolean?>?

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty?

    @TaskAction
    @Throws(IOException::class)
    fun documentConfigurationProperties() {
        val snippets = Snippets(this.configurationPropertyMetadata, this.deprecated.getOrElse(false))
        snippets.add(
            "application-properties.core",
            "Core Properties",
            Consumer { config: Snippet.Config? -> this.corePrefixes(config!!) })
        snippets.add(
            "application-properties.cache",
            "Cache Properties",
            Consumer { config: Snippet.Config? -> this.cachePrefixes(config!!) })
        snippets.add(
            "application-properties.grpc",
            "gRPC Properties",
            Consumer { prefix: Snippet.Config? -> this.grpcPrefixes(prefix!!) })
        snippets.add(
            "application-properties.mail",
            "Mail Properties",
            Consumer { config: Snippet.Config? -> this.mailPrefixes(config!!) })
        snippets.add(
            "application-properties.json",
            "JSON Properties",
            Consumer { config: Snippet.Config? -> this.jsonPrefixes(config!!) })
        snippets.add(
            "application-properties.data",
            "Data Properties",
            Consumer { config: Snippet.Config? -> this.dataPrefixes(config!!) })
        snippets.add(
            "application-properties.transaction",
            "Transaction Properties",
            Consumer { prefix: Snippet.Config? -> this.transactionPrefixes(prefix!!) })
        snippets.add(
            "application-properties.data-migration",
            "Data Migration Properties",
            Consumer { prefix: Snippet.Config? -> this.dataMigrationPrefixes(prefix!!) })
        snippets.add(
            "application-properties.integration",
            "Integration Properties",
            Consumer { prefix: Snippet.Config? -> this.integrationPrefixes(prefix!!) })
        snippets.add(
            "application-properties.web",
            "Web Properties",
            Consumer { prefix: Snippet.Config? -> this.webPrefixes(prefix!!) })
        snippets.add(
            "application-properties.templating",
            "Templating Properties",
            Consumer { prefix: Snippet.Config? -> this.templatePrefixes(prefix!!) })
        snippets.add(
            "application-properties.server",
            "Server Properties",
            Consumer { prefix: Snippet.Config? -> this.serverPrefixes(prefix!!) })
        snippets.add(
            "application-properties.security",
            "Security Properties",
            Consumer { prefix: Snippet.Config? -> this.securityPrefixes(prefix!!) })
        snippets.add(
            "application-properties.rsocket",
            "RSocket Properties",
            Consumer { prefix: Snippet.Config? -> this.rsocketPrefixes(prefix!!) })
        snippets.add(
            "application-properties.actuator",
            "Actuator Properties",
            Consumer { prefix: Snippet.Config? -> this.actuatorPrefixes(prefix!!) })
        snippets.add(
            "application-properties.devtools",
            "Devtools Properties",
            Consumer { prefix: Snippet.Config? -> this.devtoolsPrefixes(prefix!!) })
        snippets.add(
            "application-properties.docker-compose",
            "Docker Compose Properties",
            Consumer { prefix: Snippet.Config? -> this.dockerComposePrefixes(prefix!!) })
        snippets.add(
            "application-properties.testcontainers", "Testcontainers Properties",
            Consumer { prefix: Snippet.Config? -> this.testcontainersPrefixes(prefix!!) })
        snippets.add(
            "application-properties.testing",
            "Testing Properties",
            Consumer { prefix: Snippet.Config? -> this.testingPrefixes(prefix!!) })
        snippets.writeTo(this.outputDir.getAsFile().get().toPath())
    }

    private fun corePrefixes(config: Snippet.Config) {
        config.accept("debug")
        config.accept("trace")
        config.accept("logging")
        config.accept("spring.aop")
        config.accept("spring.application")
        config.accept("spring.autoconfigure")
        config.accept("spring.banner")
        config.accept("spring.beaninfo")
        config.accept("spring.config")
        config.accept("spring.info")
        config.accept("spring.jmx")
        config.accept("spring.lifecycle")
        config.accept("spring.main")
        config.accept("spring.messages")
        config.accept("spring.pid")
        config.accept("spring.profiles")
        config.accept("spring.quartz")
        config.accept("spring.reactor")
        config.accept("spring.ssl")
        config.accept("spring.task")
        config.accept("spring.threads")
        config.accept("spring.validation")
        config.accept("spring.mandatory-file-encoding")
        config.accept("info")
        config.accept("spring.output.ansi.enabled")
    }

    private fun cachePrefixes(config: Snippet.Config) {
        config.accept("spring.cache")
    }

    private fun grpcPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.grpc")
    }

    private fun mailPrefixes(config: Snippet.Config) {
        config.accept("spring.mail")
        config.accept("spring.sendgrid")
    }

    private fun jsonPrefixes(config: Snippet.Config) {
        config.accept("spring.jackson")
        config.accept("spring.gson")
        config.accept("spring.kotlinx.serialization.json")
    }

    private fun dataPrefixes(config: Snippet.Config) {
        config.accept("spring.couchbase")
        config.accept("spring.cassandra")
        config.accept("spring.elasticsearch")
        config.accept("spring.h2")
        config.accept("spring.influx")
        config.accept("spring.ldap")
        config.accept("spring.mongodb")
        config.accept("spring.neo4j")
        config.accept("spring.persistence")
        config.accept("spring.data")
        config.accept("spring.datasource")
        config.accept("spring.jooq")
        config.accept("spring.jdbc")
        config.accept("spring.jpa")
        config.accept("spring.r2dbc")
        config.accept(
            "spring.datasource.oracleucp",
            "Oracle UCP specific settings bound to an instance of Oracle UCP's PoolDataSource"
        )
        config.accept(
            "spring.datasource.dbcp2",
            "Commons DBCP2 specific settings bound to an instance of DBCP2's BasicDataSource"
        )
        config.accept(
            "spring.datasource.tomcat",
            "Tomcat datasource specific settings bound to an instance of Tomcat JDBC's DataSource"
        )
        config.accept(
            "spring.datasource.hikari",
            "Hikari specific settings bound to an instance of Hikari's HikariDataSource"
        )
    }

    private fun transactionPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.jta")
        prefix.accept("spring.transaction")
    }

    private fun dataMigrationPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.flyway")
        prefix.accept("spring.liquibase")
        prefix.accept("spring.sql.init")
    }

    private fun integrationPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.activemq")
        prefix.accept("spring.artemis")
        prefix.accept("spring.batch")
        prefix.accept("spring.integration")
        prefix.accept("spring.jms")
        prefix.accept("spring.kafka")
        prefix.accept("spring.pulsar")
        prefix.accept("spring.rabbitmq")
        prefix.accept("spring.hazelcast")
        prefix.accept("spring.webservices")
    }

    private fun webPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.graphql")
        prefix.accept("spring.hateoas")
        prefix.accept("spring.http")
        prefix.accept("spring.jersey")
        prefix.accept("spring.mvc")
        prefix.accept("spring.netty")
        prefix.accept("spring.resources")
        prefix.accept("spring.servlet")
        prefix.accept("spring.session")
        prefix.accept("spring.web")
        prefix.accept("spring.webflux")
    }

    private fun templatePrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.freemarker")
        prefix.accept("spring.groovy")
        prefix.accept("spring.mustache")
        prefix.accept("spring.thymeleaf")
    }

    private fun serverPrefixes(prefix: Snippet.Config) {
        prefix.accept("server")
    }

    private fun securityPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.security")
    }

    private fun rsocketPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.rsocket")
    }

    private fun actuatorPrefixes(prefix: Snippet.Config) {
        prefix.accept("management")
        prefix.accept("micrometer")
    }

    private fun dockerComposePrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.docker.compose")
    }

    private fun devtoolsPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.devtools")
    }

    private fun testingPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.test.")
    }

    private fun testcontainersPrefixes(prefix: Snippet.Config) {
        prefix.accept("spring.testcontainers.")
    }
}
