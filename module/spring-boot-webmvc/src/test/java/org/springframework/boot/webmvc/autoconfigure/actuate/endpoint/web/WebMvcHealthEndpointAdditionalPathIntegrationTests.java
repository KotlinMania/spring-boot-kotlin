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

package io.github.kotlinmania.spring.boot.webmvc.autoconfigure.actuate.endpoint.web;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.integrationtest.AbstractHealthEndpointAdditionalPathIntegrationTests;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.application.DiskSpaceHealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import io.github.kotlinmania.spring.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import io.github.kotlinmania.spring.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import io.github.kotlinmania.spring.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.test.context.assertj.AssertableWebApplicationContext;
import io.github.kotlinmania.spring.boot.test.context.runner.WebApplicationContextRunner;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.actuate.web.server.TomcatServletManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import io.github.kotlinmania.spring.boot.web.server.context.ServerPortInfoApplicationContextInitializer;
import io.github.kotlinmania.spring.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.web.context.ConfigurableWebApplicationContext;

/**
 * Integration tests for MVC health groups on an additional path.
 *
 * @author Madhura Bhave
 */
class WebMvcHealthEndpointAdditionalPathIntegrationTests extends
		AbstractHealthEndpointAdditionalPathIntegrationTests<WebApplicationContextRunner, ConfigurableWebApplicationContext, AssertableWebApplicationContext> {

	WebMvcHealthEndpointAdditionalPathIntegrationTests() {
		super(new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
					HealthContributorAutoConfiguration.class, HealthContributorRegistryAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, ManagementContextAutoConfiguration.class,
					TomcatServletWebServerAutoConfiguration.class, TomcatServletWebServerAutoConfiguration.class,
					TomcatServletManagementContextAutoConfiguration.class, WebMvcAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					EndpointAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
					HealthEndpointAutoConfiguration.class, WebMvcHealthEndpointExtensionAutoConfiguration.class,
					DiskSpaceHealthContributorAutoConfiguration.class))
			.withInitializer(new ServerPortInfoApplicationContextInitializer())
			.withPropertyValues("server.port=0"));
	}

}
