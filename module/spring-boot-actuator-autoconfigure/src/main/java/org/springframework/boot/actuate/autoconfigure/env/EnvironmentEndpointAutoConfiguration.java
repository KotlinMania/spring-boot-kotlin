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

package io.github.kotlinmania.spring.boot.actuate.autoconfigure.env;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import io.github.kotlinmania.spring.boot.actuate.endpoint.SanitizingFunction;
import io.github.kotlinmania.spring.boot.actuate.env.EnvironmentEndpoint;
import io.github.kotlinmania.spring.boot.actuate.env.EnvironmentEndpointWebExtension;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link EnvironmentEndpoint}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(EnvironmentEndpoint.class)
@EnableConfigurationProperties(EnvironmentEndpointProperties.class)
public final class EnvironmentEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	EnvironmentEndpoint environmentEndpoint(Environment environment, EnvironmentEndpointProperties properties,
			ObjectProvider<SanitizingFunction> sanitizingFunctions) {
		return new EnvironmentEndpoint(environment, sanitizingFunctions.orderedStream().toList(),
				properties.getShowValues());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(EnvironmentEndpoint.class)
	@ConditionalOnAvailableEndpoint(exposure = EndpointExposure.WEB)
	EnvironmentEndpointWebExtension environmentEndpointWebExtension(EnvironmentEndpoint environmentEndpoint,
			EnvironmentEndpointProperties properties) {
		return new EnvironmentEndpointWebExtension(environmentEndpoint, properties.getShowValues(),
				properties.getRoles());
	}

}
