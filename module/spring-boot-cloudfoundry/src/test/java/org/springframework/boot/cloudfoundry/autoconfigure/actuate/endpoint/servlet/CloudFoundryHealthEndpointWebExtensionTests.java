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

package io.github.kotlinmania.spring.boot.cloudfoundry.autoconfigure.actuate.endpoint.servlet;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.endpoint.ApiVersion;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthDescriptor;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import io.github.kotlinmania.spring.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.contributor.Health;
import io.github.kotlinmania.spring.boot.health.contributor.HealthIndicator;
import io.github.kotlinmania.spring.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import io.github.kotlinmania.spring.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import io.github.kotlinmania.spring.boot.restclient.autoconfigure.RestTemplateAutoConfiguration;
import io.github.kotlinmania.spring.boot.security.autoconfigure.SecurityAutoConfiguration;
import io.github.kotlinmania.spring.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import io.github.kotlinmania.spring.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.test.context.runner.WebApplicationContextRunner;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryHealthEndpointWebExtension}.
 *
 * @author Madhura Bhave
 */
class CloudFoundryHealthEndpointWebExtensionTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withPropertyValues("VCAP_APPLICATION={}")
		.withConfiguration(
				AutoConfigurations.of(SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class,
						WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
						DispatcherServletAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
						PropertyPlaceholderAutoConfiguration.class, RestTemplateAutoConfiguration.class,
						ManagementContextAutoConfiguration.class, ServletManagementContextAutoConfiguration.class,
						EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
						HealthContributorAutoConfiguration.class, HealthContributorRegistryAutoConfiguration.class,
						HealthEndpointAutoConfiguration.class, CloudFoundryActuatorAutoConfiguration.class))
		.withUserConfiguration(TestHealthIndicator.class);

	@Test
	void healthComponentsAlwaysPresent() {
		this.contextRunner.run((context) -> {
			CloudFoundryHealthEndpointWebExtension extension = context
				.getBean(CloudFoundryHealthEndpointWebExtension.class);
			HealthDescriptor descriptor = extension.health(ApiVersion.V3).getBody();
			assertThat(descriptor).isNotNull();
			Map<String, HealthDescriptor> components = ((CompositeHealthDescriptor) descriptor).getComponents();
			assertThat(components).isNotNull();
			HealthDescriptor component = components.entrySet().iterator().next().getValue();
			assertThat(((IndicatedHealthDescriptor) component).getDetails()).containsEntry("spring", "boot");
		});
	}

	private static final class TestHealthIndicator implements HealthIndicator {

		@Override
		public Health health() {
			return Health.up().withDetail("spring", "boot").build();
		}

	}

}
