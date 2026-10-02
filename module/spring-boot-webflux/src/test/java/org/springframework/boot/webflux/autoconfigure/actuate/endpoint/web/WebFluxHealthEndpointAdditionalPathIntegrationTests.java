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

package io.github.kotlinmania.spring.boot.webflux.autoconfigure.actuate.endpoint.web;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.integrationtest.AbstractHealthEndpointAdditionalPathIntegrationTests;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.application.DiskSpaceHealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import io.github.kotlinmania.spring.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import io.github.kotlinmania.spring.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import io.github.kotlinmania.spring.boot.reactor.netty.autoconfigure.NettyReactiveWebServerAutoConfiguration;
import io.github.kotlinmania.spring.boot.reactor.netty.autoconfigure.actuate.web.server.NettyReactiveManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import io.github.kotlinmania.spring.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import io.github.kotlinmania.spring.boot.web.context.reactive.ConfigurableReactiveWebApplicationContext;
import io.github.kotlinmania.spring.boot.web.server.context.ServerPortInfoApplicationContextInitializer;
import io.github.kotlinmania.spring.boot.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import io.github.kotlinmania.spring.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
import io.github.kotlinmania.spring.boot.webflux.autoconfigure.WebFluxAutoConfiguration;

/**
 * Integration tests for Webflux health groups on an additional path.
 *
 * @author Madhura Bhave
 */
class WebFluxHealthEndpointAdditionalPathIntegrationTests extends
		AbstractHealthEndpointAdditionalPathIntegrationTests<ReactiveWebApplicationContextRunner, ConfigurableReactiveWebApplicationContext, AssertableReactiveWebApplicationContext> {

	WebFluxHealthEndpointAdditionalPathIntegrationTests() {
		super(new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, CodecsAutoConfiguration.class,
					WebFluxAutoConfiguration.class, HealthContributorAutoConfiguration.class,
					HealthContributorRegistryAutoConfiguration.class, HttpHandlerAutoConfiguration.class,
					EndpointAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
					WebFluxHealthEndpointExtensionAutoConfiguration.class,
					DiskSpaceHealthContributorAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					ManagementContextAutoConfiguration.class, NettyReactiveWebServerAutoConfiguration.class,
					NettyReactiveManagementContextAutoConfiguration.class, BeansEndpointAutoConfiguration.class))
			.withInitializer(new ServerPortInfoApplicationContextInitializer())
			.withPropertyValues("server.port=0"));
	}

}
