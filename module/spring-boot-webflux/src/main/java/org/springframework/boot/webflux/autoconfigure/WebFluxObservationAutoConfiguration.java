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

package io.github.kotlinmania.spring.boot.webflux.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.micrometer.metrics.MaximumAllowableTagsMeterFilter;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.MetricsProperties;
import io.github.kotlinmania.spring.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring
 * WebFlux applications.
 *
 * @author Brian Clozel
 * @author Jon Schneider
 * @author Dmytro Nosan
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(afterName = {
		"io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration",
		"io.github.kotlinmania.spring.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration" })
@ConditionalOnClass({ Observation.class, MeterRegistry.class })
@ConditionalOnBean({ ObservationRegistry.class, MeterRegistry.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public final class WebFluxObservationAutoConfiguration {

	private final ObservationProperties observationProperties;

	WebFluxObservationAutoConfiguration(ObservationProperties observationProperties) {
		this.observationProperties = observationProperties;
	}

	@Bean
	@Order(0)
	MaximumAllowableTagsMeterFilter metricsHttpServerUriTagFilter(MetricsProperties metricsProperties) {
		String meterNamePrefix = this.observationProperties.getHttp().getServer().getRequests().getName();
		int maxUriTags = metricsProperties.getWeb().getServer().getMaxUriTags();
		return new MaximumAllowableTagsMeterFilter(meterNamePrefix, "uri", maxUriTags);
	}

	@Bean
	@ConditionalOnMissingBean(ServerRequestObservationConvention.class)
	DefaultServerRequestObservationConvention defaultServerRequestObservationConvention() {
		return new DefaultServerRequestObservationConvention(
				this.observationProperties.getHttp().getServer().getRequests().getName());
	}

}
