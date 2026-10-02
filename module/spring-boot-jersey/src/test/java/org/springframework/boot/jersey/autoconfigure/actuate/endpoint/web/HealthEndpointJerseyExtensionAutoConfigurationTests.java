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

package io.github.kotlinmania.spring.boot.jersey.autoconfigure.actuate.endpoint.web;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.WithTestEndpointOutcomeExposureContributor;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebEndpointsSupplier;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpoint;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpointWebExtension;
import io.github.kotlinmania.spring.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.contributor.Health;
import io.github.kotlinmania.spring.boot.health.contributor.HealthIndicator;
import io.github.kotlinmania.spring.boot.health.contributor.ReactiveHealthIndicator;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.actuate.endpoint.web.HealthEndpointJerseyExtensionAutoConfiguration.JerseyAdditionalHealthEndpointPathsResourcesRegistrar;
import io.github.kotlinmania.spring.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthEndpointJerseyExtensionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class HealthEndpointJerseyExtensionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
				HealthContributorRegistryAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				HealthEndpointJerseyExtensionAutoConfiguration.class));

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void additionalJerseyHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.contextRunner
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.test.exposure.include=*")
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(HealthEndpoint.class);
				assertThat(context).hasSingleBean(HealthEndpointWebExtension.class);
				assertThat(context.getBean(WebEndpointsSupplier.class).getEndpoints()).isEmpty();
				assertThat(context).hasSingleBean(JerseyAdditionalHealthEndpointPathsResourcesRegistrar.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class HealthIndicatorsConfiguration {

		@Bean
		HealthIndicator simpleHealthIndicator() {
			return () -> Health.up().withDetail("counter", 42).build();
		}

		@Bean
		HealthIndicator additionalHealthIndicator() {
			return () -> Health.up().build();
		}

		@Bean
		ReactiveHealthIndicator reactiveHealthIndicator() {
			return () -> Mono.just(Health.up().build());
		}

	}

}
