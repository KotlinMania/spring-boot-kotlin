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

import java.util.Collection;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointMapping;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.ExposableWebEndpoint;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebEndpointsSupplier;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebServerNamespace;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpoint;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpointGroups;
import io.github.kotlinmania.spring.boot.health.contributor.Health;
import io.github.kotlinmania.spring.boot.webflux.actuate.endpoint.web.AdditionalHealthEndpointPathsWebFluxHandlerMapping;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthEndpoint} web
 * extension with Spring WebFlux.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass({ HealthEndpoint.class, Health.class })
@ConditionalOnBean({ WebEndpointsSupplier.class, HealthEndpointGroups.class })
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
public final class WebFluxHealthEndpointExtensionAutoConfiguration {

	@Bean
	AdditionalHealthEndpointPathsWebFluxHandlerMapping healthEndpointWebFluxHandlerMapping(
			WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups groups) {
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		ExposableWebEndpoint health = webEndpoints.stream()
			.filter((endpoint) -> endpoint.getEndpointId().equals(HealthEndpoint.ID))
			.findFirst()
			.orElse(null);
		return new AdditionalHealthEndpointPathsWebFluxHandlerMapping(new EndpointMapping(""), health,
				groups.getAllWithAdditionalPath(WebServerNamespace.SERVER));
	}

}
