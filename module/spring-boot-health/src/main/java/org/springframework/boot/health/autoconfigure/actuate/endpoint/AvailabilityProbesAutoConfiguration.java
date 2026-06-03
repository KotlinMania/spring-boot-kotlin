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

package io.github.kotlinmania.spring.boot.health.autoconfigure.actuate.endpoint;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.availability.ApplicationAvailability;
import io.github.kotlinmania.spring.boot.health.application.LivenessStateHealthIndicator;
import io.github.kotlinmania.spring.boot.health.application.ReadinessStateHealthIndicator;
import io.github.kotlinmania.spring.boot.health.autoconfigure.application.AvailabilityHealthContributorAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for availability probes.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = { AvailabilityHealthContributorAutoConfiguration.class,
		ApplicationAvailabilityAutoConfiguration.class })
@ConditionalOnClass(name = "io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.Endpoint")
@ConditionalOnBooleanProperty(name = "management.endpoint.health.probes.enabled", matchIfMissing = true)
public final class AvailabilityProbesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "livenessStateHealthIndicator")
	LivenessStateHealthIndicator livenessStateHealthIndicator(ApplicationAvailability applicationAvailability) {
		return new LivenessStateHealthIndicator(applicationAvailability);
	}

	@Bean
	@ConditionalOnMissingBean(name = "readinessStateHealthIndicator")
	ReadinessStateHealthIndicator readinessStateHealthIndicator(ApplicationAvailability applicationAvailability) {
		return new ReadinessStateHealthIndicator(applicationAvailability);
	}

	@Bean
	AvailabilityProbesHealthEndpointGroupsPostProcessor availabilityProbesHealthEndpointGroupsPostProcessor(
			Environment environment) {
		return new AvailabilityProbesHealthEndpointGroupsPostProcessor(environment);
	}

}
