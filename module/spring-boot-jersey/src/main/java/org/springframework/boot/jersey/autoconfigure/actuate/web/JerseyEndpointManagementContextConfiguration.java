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

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import io.github.kotlinmania.spring.boot.actuate.endpoint.EndpointAccessResolver;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.JerseyApplicationPath;
import org.springframework.context.annotation.Bean;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Jetty
 * endpoints.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 4.0.0
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
public class JerseyEndpointManagementContextConfiguration {

	@Bean
	@SuppressWarnings("removal")
	public io.github.kotlinmania.spring.boot.actuate.endpoint.web.ServletEndpointRegistrar servletEndpointRegistrar(
			WebEndpointProperties properties,
			io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier servletEndpointsSupplier,
			JerseyApplicationPath jerseyApplicationPath, EndpointAccessResolver endpointAccessResolver) {
		return new io.github.kotlinmania.spring.boot.actuate.endpoint.web.ServletEndpointRegistrar(
				jerseyApplicationPath.getRelativePath(properties.getBasePath()),
				servletEndpointsSupplier.getEndpoints(), endpointAccessResolver);
	}

}
