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

package io.github.kotlinmania.spring.boot.r2dbc.docker.compose;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.condition.OS;

import io.github.kotlinmania.spring.boot.docker.compose.service.connection.test.DockerComposeTest;
import io.github.kotlinmania.spring.boot.jdbc.DatabaseDriver;
import io.github.kotlinmania.spring.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import io.github.kotlinmania.spring.boot.testsupport.container.TestImage;
import io.github.kotlinmania.spring.boot.testsupport.junit.DisabledOnOs;
import org.springframework.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleXeR2dbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@DisabledOnOs(os = { OS.LINUX, OS.MAC }, architecture = "aarch64",
		disabledReason = "The Oracle image has no ARM support")
class OracleXeR2dbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "oracle-compose.yaml", image = TestImage.ORACLE_XE)
	void runCreatesConnectionDetailsThatCanBeUsedToAccessDatabase(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		assertThat(connectionFactoryOptions.toString()).contains("database=xepdb1", "driver=oracle",
				"password=REDACTED", "user=app_user");
		assertThat(connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.PASSWORD))
			.isEqualTo("app_user_secret");
		Awaitility.await().atMost(Duration.ofMinutes(1)).ignoreExceptions().untilAsserted(() -> {
			String validationQuery = DatabaseDriver.ORACLE.getValidationQuery();
			assertThat(validationQuery).isNotNull();
			Object result = DatabaseClient.create(ConnectionFactories.get(connectionFactoryOptions))
				.sql(validationQuery)
				.map((row, metadata) -> row.get(0))
				.first()
				.block(Duration.ofSeconds(30));
			assertThat(result).isEqualTo("Hello");
		});
	}

}
