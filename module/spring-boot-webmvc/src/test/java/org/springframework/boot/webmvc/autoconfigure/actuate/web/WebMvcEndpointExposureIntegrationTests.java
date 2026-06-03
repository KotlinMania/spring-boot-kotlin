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

package io.github.kotlinmania.spring.boot.webmvc.autoconfigure.actuate.web;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.actuate.audit.InMemoryAuditEventRepository;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.context.ShutdownEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import io.github.kotlinmania.spring.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import io.github.kotlinmania.spring.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import io.github.kotlinmania.spring.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.servlet.autoconfigure.actuate.web.exchanges.ServletHttpExchangesAutoConfiguration;
import io.github.kotlinmania.spring.boot.test.context.assertj.AssertableWebApplicationContext;
import io.github.kotlinmania.spring.boot.test.context.runner.WebApplicationContextRunner;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import io.github.kotlinmania.spring.boot.web.server.WebServer;
import io.github.kotlinmania.spring.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import io.github.kotlinmania.spring.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Endpoints over Spring MVC.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class WebMvcEndpointExposureIntegrationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
		.withConfiguration(AutoConfigurations.of(TomcatServletWebServerAutoConfiguration.class,
				TomcatServletWebServerAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
				JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
				WebMvcAutoConfiguration.class, EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
				ManagementContextAutoConfiguration.class, ManagementContextAutoConfiguration.class,
				ServletManagementContextAutoConfiguration.class, ServletHttpExchangesAutoConfiguration.class,
				HealthContributorAutoConfiguration.class, HealthContributorRegistryAutoConfiguration.class,
				BeansEndpointAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				ShutdownEndpointAutoConfiguration.class))
		.withUserConfiguration(CustomMvcEndpoint.class, CustomServletEndpoint.class,
				HttpExchangeRepositoryConfiguration.class, AuditEventRepositoryConfiguration.class)
		.withPropertyValues("server.port:0");

	@Test
	void webEndpointsExceptHealthAreDisabledByDefault() {
		this.contextRunner.run((context) -> {
			RestClient client = createClient(context);
			assertThat(isExposed(client, HttpMethod.GET, "beans")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "health")).isTrue();
			assertThat(isExposed(client, HttpMethod.POST, "shutdown")).isFalse();
		});
	}

	@Test
	void webEndpointsCanBeExposed() {
		WebApplicationContextRunner contextRunner = this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*");
		contextRunner.run((context) -> {
			RestClient client = createClient(context);
			assertThat(isExposed(client, HttpMethod.GET, "beans")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "health")).isTrue();
			assertThat(isExposed(client, HttpMethod.POST, "shutdown")).isFalse();
		});
	}

	@Test
	void singleWebEndpointCanBeExposed() {
		WebApplicationContextRunner contextRunner = this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=beans");
		contextRunner.run((context) -> {
			RestClient client = createClient(context);
			assertThat(isExposed(client, HttpMethod.GET, "beans")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "health")).isFalse();
			assertThat(isExposed(client, HttpMethod.POST, "shutdown")).isFalse();
		});
	}

	@Test
	void singleWebEndpointCanBeExcluded() {
		WebApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues(
				"management.endpoints.web.exposure.include=*", "management.endpoints.web.exposure.exclude=beans");
		contextRunner.run((context) -> {
			RestClient client = createClient(context);
			assertThat(isExposed(client, HttpMethod.GET, "beans")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "health")).isTrue();
			assertThat(isExposed(client, HttpMethod.POST, "shutdown")).isFalse();
		});
	}

	private RestClient createClient(AssertableWebApplicationContext context) {
		WebServer webServer = context.getSourceApplicationContext(ServletWebServerApplicationContext.class)
			.getWebServer();
		assertThat(webServer).isNotNull();
		int port = webServer.getPort();
		return RestClient.builder().defaultStatusHandler((status) -> true, (request, response) -> {
		}).baseUrl("http://localhost:" + port).build();
	}

	private boolean isExposed(RestClient client, HttpMethod method, String path) {
		path = "/actuator/" + path;
		ResponseEntity<byte[]> result = client.method(method).uri(path).retrieve().toEntity(byte[].class);
		if (result.getStatusCode() == HttpStatus.OK) {
			return true;
		}
		if (result.getStatusCode() == HttpStatus.NOT_FOUND) {
			return false;
		}
		throw new IllegalStateException(
				String.format("Unexpected %s HTTP status for endpoint %s", result.getStatusCode(), path));
	}

	@io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.RestControllerEndpoint(id = "custommvc")
	@SuppressWarnings("removal")
	static class CustomMvcEndpoint {

		@GetMapping("/")
		String main() {
			return "test";
		}

	}

	@io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.ServletEndpoint(id = "customservlet")
	@SuppressWarnings({ "deprecation", "removal" })
	static class CustomServletEndpoint
			implements Supplier<io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointServlet> {

		@Override
		public io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointServlet get() {
			return new io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointServlet(new HttpServlet() {

				@Override
				protected void doGet(HttpServletRequest req, HttpServletResponse resp)
						throws ServletException, IOException {
				}

			});
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpExchangeRepositoryConfiguration {

		@Bean
		InMemoryHttpExchangeRepository httpExchangeRepository() {
			return new InMemoryHttpExchangeRepository();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AuditEventRepositoryConfiguration {

		@Bean
		InMemoryAuditEventRepository auditEventRepository() {
			return new InMemoryAuditEventRepository();
		}

	}

}
