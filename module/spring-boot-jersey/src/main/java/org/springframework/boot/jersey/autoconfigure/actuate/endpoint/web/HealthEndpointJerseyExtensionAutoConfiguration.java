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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointMapping;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.ExposableWebEndpoint;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebEndpointsSupplier;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebServerNamespace;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpoint;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpointGroups;
import io.github.kotlinmania.spring.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.jersey.actuate.endpoint.web.JerseyHealthEndpointAdditionalPathResourceFactory;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.DefaultJerseyApplicationPath;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.JerseyApplicationPath;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.JerseyProperties;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.ResourceConfigCustomizer;
import io.github.kotlinmania.spring.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-Configuration for {@link HealthEndpoint} Jersey extension.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0.0
 * @see HealthEndpointAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnBean(HealthEndpoint.class)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
public final class HealthEndpointJerseyExtensionAutoConfiguration {

	@Bean
	JerseyAdditionalHealthEndpointPathsResourcesRegistrar jerseyAdditionalHealthEndpointPathsResourcesRegistrar(
			WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups healthEndpointGroups) {
		ExposableWebEndpoint health = getHealthEndpoint(webEndpointsSupplier);
		return new JerseyAdditionalHealthEndpointPathsResourcesRegistrar(health, healthEndpointGroups);
	}

	private static @Nullable ExposableWebEndpoint getHealthEndpoint(WebEndpointsSupplier webEndpointsSupplier) {
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		return webEndpoints.stream()
			.filter((endpoint) -> endpoint.getEndpointId().equals(HealthEndpoint.ID))
			.findFirst()
			.orElse(null);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ResourceConfig.class)
	@EnableConfigurationProperties(JerseyProperties.class)
	static class JerseyInfrastructureConfiguration {

		@Bean
		@ConditionalOnMissingBean
		JerseyApplicationPath jerseyApplicationPath(JerseyProperties properties, ResourceConfig config) {
			return new DefaultJerseyApplicationPath(properties.getApplicationPath(), config);
		}

		@Bean
		ResourceConfig resourceConfig(ObjectProvider<ResourceConfigCustomizer> resourceConfigCustomizers) {
			ResourceConfig resourceConfig = new ResourceConfig();
			resourceConfigCustomizers.orderedStream().forEach((customizer) -> customizer.customize(resourceConfig));
			return resourceConfig;
		}

		@Bean
		ServletRegistrationBean<ServletContainer> jerseyServletRegistration(JerseyApplicationPath jerseyApplicationPath,
				ResourceConfig resourceConfig) {
			return new ServletRegistrationBean<>(new ServletContainer(resourceConfig),
					jerseyApplicationPath.getUrlMapping());
		}

	}

	static class JerseyAdditionalHealthEndpointPathsResourcesRegistrar implements ResourceConfigCustomizer {

		private final @Nullable ExposableWebEndpoint endpoint;

		private final HealthEndpointGroups groups;

		JerseyAdditionalHealthEndpointPathsResourcesRegistrar(@Nullable ExposableWebEndpoint endpoint,
				HealthEndpointGroups groups) {
			this.endpoint = endpoint;
			this.groups = groups;
		}

		@Override
		public void customize(ResourceConfig config) {
			register(config);
		}

		private void register(ResourceConfig config) {
			EndpointMapping mapping = new EndpointMapping("");
			JerseyHealthEndpointAdditionalPathResourceFactory resourceFactory = new JerseyHealthEndpointAdditionalPathResourceFactory(
					WebServerNamespace.SERVER, this.groups);
			Collection<Resource> endpointResources = resourceFactory
				.createEndpointResources(mapping,
						(this.endpoint != null) ? Collections.singletonList(this.endpoint) : Collections.emptyList())
				.stream()
				.filter(Objects::nonNull)
				.toList();
			register(endpointResources, config);
		}

		private void register(Collection<Resource> resources, ResourceConfig config) {
			config.registerResources(new HashSet<>(resources));
		}

	}

}
