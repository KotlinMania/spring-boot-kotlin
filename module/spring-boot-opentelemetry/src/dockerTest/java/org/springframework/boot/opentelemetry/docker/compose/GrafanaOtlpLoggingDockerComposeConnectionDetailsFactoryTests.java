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

package io.github.kotlinmania.spring.boot.opentelemetry.docker.compose;

import io.github.kotlinmania.spring.boot.docker.compose.service.connection.test.DockerComposeTest;
import io.github.kotlinmania.spring.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import io.github.kotlinmania.spring.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import io.github.kotlinmania.spring.boot.ssl.SslBundle;
import io.github.kotlinmania.spring.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OtlpLoggingDockerComposeConnectionDetailsFactory} using
 * {@link TestImage#GRAFANA_OTEL_LGTM}.
 *
 * @author Eddú Meléndez
 */
class GrafanaOtlpLoggingDockerComposeConnectionDetailsFactoryTests {

	@DockerComposeTest(composeFile = "otlp-compose.yaml", image = TestImage.GRAFANA_OTEL_LGTM)
	void runCreatesConnectionDetails(OtlpLoggingConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl(Transport.HTTP)).startsWith("http://").endsWith("/v1/logs");
		assertThat(connectionDetails.getUrl(Transport.GRPC)).startsWith("http://").endsWith("/v1/logs");
		assertThat(connectionDetails.getSslBundle()).isNull();
	}

	@DockerComposeTest(composeFile = "otlp-ssl-compose.yaml", image = TestImage.GRAFANA_OTEL_LGTM,
			additionalResources = "ca.crt")
	void runWithSslCreatesConnectionDetails(OtlpLoggingConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrl(Transport.HTTP)).startsWith("https://").endsWith("/v1/logs");
		assertThat(connectionDetails.getUrl(Transport.GRPC)).startsWith("https://").endsWith("/v1/logs");
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

}
