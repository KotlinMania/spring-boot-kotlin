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

package io.github.kotlinmania.spring.boot.jersey.autoconfigure.actuate.web;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import io.github.kotlinmania.spring.boot.actuate.endpoint.Access;
import io.github.kotlinmania.spring.boot.actuate.endpoint.EndpointAccessResolver;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.ServletEndpointRegistrar;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.JerseyApplicationPath;
import io.github.kotlinmania.spring.boot.test.context.runner.ApplicationContextRunner;
import io.github.kotlinmania.spring.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyEndpointManagementContextConfiguration}.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("removal")
class JerseyEndpointManagementContextConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(TestConfig.class);

	@Test
	void contextShouldContainServletEndpointRegistrar() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ServletEndpointRegistrar.class);
			ServletEndpointRegistrar bean = context.getBean(ServletEndpointRegistrar.class);
			assertThat(bean).hasFieldOrPropertyWithValue("basePath", "/test/actuator");
		});
	}

	@Test
	void contextWhenNotServletBasedShouldNotContainServletEndpointRegistrar() {
		new ApplicationContextRunner().withUserConfiguration(TestConfig.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ServletEndpointRegistrar.class));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(JerseyEndpointManagementContextConfiguration.class)
	@EnableConfigurationProperties(WebEndpointProperties.class)
	static class TestConfig {

		@Bean
		ServletEndpointsSupplier servletEndpointsSupplier() {
			return Collections::emptyList;
		}

		@Bean
		JerseyApplicationPath jerseyApplicationPath() {
			return () -> "/test";
		}

		@Bean
		EndpointAccessResolver endpointAccessResolver() {
			return (endpointId, defaultAccess) -> Access.UNRESTRICTED;
		}

	}

}
