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

package io.github.kotlinmania.spring.boot.http.client.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.micrometer.metrics.MaximumAllowableTagsMeterFilter;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.MetricsProperties;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.MetricsProperties.Web.Client;
import io.github.kotlinmania.spring.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP client-related metrics.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Brian Clozel
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ ObservationProperties.class, MeterRegistry.class, MetricsProperties.class })
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public final class HttpClientMetricsAutoConfiguration {

	@Bean
	@Order(0)
	MaximumAllowableTagsMeterFilter metricsHttpClientUriTagFilter(ObservationProperties observationProperties,
			MetricsProperties metricsProperties) {
		Client clientProperties = metricsProperties.getWeb().getClient();
		String meterNamePrefix = observationProperties.getHttp().getClient().getRequests().getName();
		int maxUriTags = clientProperties.getMaxUriTags();
		return new MaximumAllowableTagsMeterFilter(meterNamePrefix, "uri", maxUriTags, "Are you using 'uriVariables'?");
	}

}
