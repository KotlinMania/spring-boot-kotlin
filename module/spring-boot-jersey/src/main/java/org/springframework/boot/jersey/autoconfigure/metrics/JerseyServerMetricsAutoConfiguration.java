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

package io.github.kotlinmania.spring.boot.jersey.autoconfigure.metrics;

import io.micrometer.observation.ObservationRegistry;
import org.glassfish.jersey.micrometer.server.JerseyObservationConvention;
import org.glassfish.jersey.micrometer.server.ObservationApplicationEventListener;
import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.ResourceConfigCustomizer;
import io.github.kotlinmania.spring.boot.micrometer.metrics.MaximumAllowableTagsMeterFilter;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.MetricsProperties;
import io.github.kotlinmania.spring.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Jersey server instrumentation.
 *
 * @author Michael Weirauch
 * @author Michael Simons
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "io.github.kotlinmania.spring.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ ResourceConfig.class, ObservationApplicationEventListener.class })
@ConditionalOnBean({ ResourceConfig.class, ObservationRegistry.class })
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public final class JerseyServerMetricsAutoConfiguration {

	private final ObservationProperties observationProperties;

	JerseyServerMetricsAutoConfiguration(ObservationProperties observationProperties) {
		this.observationProperties = observationProperties;
	}

	@Bean
	ResourceConfigCustomizer jerseyServerObservationResourceConfigCustomizer(ObservationRegistry observationRegistry,
			ObjectProvider<JerseyObservationConvention> jerseyObservationConvention) {
		String metricName = this.observationProperties.getHttp().getServer().getRequests().getName();
		return (config) -> config.register(new ObservationApplicationEventListener(observationRegistry, metricName,
				jerseyObservationConvention.getIfAvailable()));
	}

	@Bean
	@Order(0)
	MaximumAllowableTagsMeterFilter jerseyMetricsUriTagFilter(MetricsProperties metricsProperties) {
		String meterNamePrefix = this.observationProperties.getHttp().getServer().getRequests().getName();
		int maxUriTags = metricsProperties.getWeb().getServer().getMaxUriTags();
		return new MaximumAllowableTagsMeterFilter(meterNamePrefix, "uri", maxUriTags);
	}

}
