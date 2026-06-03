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

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.endpoint.Access;
import io.github.kotlinmania.spring.boot.actuate.endpoint.EndpointAccessResolver;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebEndpointsSupplier;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.actuate.web.JerseyWebEndpointManagementContextConfiguration.JerseyWebEndpointsResourcesRegistrar;
import io.github.kotlinmania.spring.boot.test.context.FilteredClassLoader;
import io.github.kotlinmania.spring.boot.test.context.runner.ApplicationContextRunner;
import io.github.kotlinmania.spring.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyWebEndpointManagementContextConfiguration}.
 *
 * @author Michael Simons
 * @author Madhura Bhave
 */
class JerseyWebEndpointManagementContextConfigurationTests {

	private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebEndpointAutoConfiguration.class,
				JerseyWebEndpointManagementContextConfiguration.class))
		.withBean(WebEndpointsSupplier.class, () -> Collections::emptyList)
		.withBean(EndpointAccessResolver.class, () -> (endpointId, defaultAccess) -> Access.UNRESTRICTED);

	@Test
	void jerseyWebEndpointsResourcesRegistrarForEndpointsIsAutoConfigured() {
		this.runner.run((context) -> assertThat(context).hasSingleBean(JerseyWebEndpointsResourcesRegistrar.class));
	}

	@Test
	void autoConfigurationIsConditionalOnServletWebApplication() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JerseySameManagementContextConfiguration.class));
		contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(JerseySameManagementContextConfiguration.class));
	}

	@Test
	void autoConfigurationIsConditionalOnClassResourceConfig() {
		this.runner.withClassLoader(new FilteredClassLoader(ResourceConfig.class))
			.run((context) -> assertThat(context).doesNotHaveBean(JerseySameManagementContextConfiguration.class));
	}

}
