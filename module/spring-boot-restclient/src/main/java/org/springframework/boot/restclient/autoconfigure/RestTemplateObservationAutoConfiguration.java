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

package io.github.kotlinmania.spring.boot.restclient.autoconfigure;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.micrometer.observation.autoconfigure.ObservationProperties;
import io.github.kotlinmania.spring.boot.restclient.RestTemplateBuilder;
import io.github.kotlinmania.spring.boot.restclient.observation.ObservationRestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.web.client.RestTemplate;

/**
 * Configure the instrumentation of {@link RestTemplate}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ RestTemplate.class, ObservationRestTemplateCustomizer.class, ObservationRegistry.class,
		ObservationProperties.class })
@ConditionalOnBean({ ObservationRegistry.class, RestTemplateBuilder.class })
@EnableConfigurationProperties(ObservationProperties.class)
public final class RestTemplateObservationAutoConfiguration {

	@Bean
	ObservationRestTemplateCustomizer observationRestTemplateCustomizer(ObservationRegistry observationRegistry,
			ObjectProvider<ClientRequestObservationConvention> customConvention,
			ObservationProperties observationProperties) {
		String name = observationProperties.getHttp().getClient().getRequests().getName();
		ClientRequestObservationConvention observationConvention = customConvention
			.getIfAvailable(() -> new DefaultClientRequestObservationConvention(name));
		return new ObservationRestTemplateCustomizer(observationRegistry, observationConvention);
	}

}
