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

package io.github.kotlinmania.spring.boot.grpc.server.autoconfigure.health;

import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.protobuf.services.HealthStatusManager;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.AnyNestedCondition;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.grpc.server.autoconfigure.health.GrpcServerHealthAutoConfiguration.NotDisabledAndHasBindableServiceOrExplicitlyEnabledCondition;
import io.github.kotlinmania.spring.boot.grpc.server.health.GrpcServerHealth;
import io.github.kotlinmania.spring.boot.grpc.server.health.HealthCheckedGrpcComponents;
import io.github.kotlinmania.spring.boot.grpc.server.health.StatusAggregator;
import io.github.kotlinmania.spring.boot.grpc.server.health.StatusMapper;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.HealthContributorMembershipValidator;
import io.github.kotlinmania.spring.boot.health.registry.HealthContributorRegistry;
import io.github.kotlinmania.spring.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side health service.
 *
 * @author Daniel Theuke
 * @author Chris Bono
 * @author Phillip Webb
 * @since 4.1.0
 */
@AutoConfiguration(
		afterName = "io.github.kotlinmania.spring.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration")
@ConditionalOnClass({ GrpcServerFactory.class, Grpc.class, HealthStatusManager.class })
@ConditionalOnBooleanProperty(name = "spring.grpc.server.enabled", matchIfMissing = true)
@Conditional(NotDisabledAndHasBindableServiceOrExplicitlyEnabledCondition.class)
@EnableConfigurationProperties(GrpcServerHealthProperties.class)
public final class GrpcServerHealthAutoConfiguration {

	@Bean(destroyMethod = "enterTerminalState")
	@ConditionalOnMissingBean
	HealthStatusManager grpcServerHealthStatusManager() {
		return new HealthStatusManager();
	}

	@Bean
	BindableService grpcServerHealthService(HealthStatusManager healthStatusManager) {
		return healthStatusManager.getHealthService();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(type = "io.github.kotlinmania.spring.boot.health.registry.HealthContributorRegistry")
	static class GrpcServerHealthContributorConfiguration {

		static final String VALIDATE_MEMBERSHIP_PROPERTY = "spring.grpc.server.health.services.validate-membership";

		@Bean
		@ConditionalOnMissingBean
		StatusAggregator grpcServerHealthStatusAggregator(GrpcServerHealthProperties properties) {
			return StatusAggregator.of(properties.getStatus().getOrder());
		}

		@Bean
		@ConditionalOnMissingBean
		StatusMapper grpcServerHealthHttpCodeStatusMapper(GrpcServerHealthProperties properties) {
			return StatusMapper.of(properties.getStatus().getMapping());
		}

		@Bean
		@ConditionalOnMissingBean(HealthCheckedGrpcComponents.class)
		AutoConfiguredHealthCheckedGrpcComponents grpcServerHealthCheckedGrpcComponents(
				ApplicationContext applicationContext, GrpcServerHealthProperties properties) {
			return new AutoConfiguredHealthCheckedGrpcComponents(applicationContext, properties);
		}

		@Bean
		@ConditionalOnMissingBean
		GrpcServerHealth grpcServerHealth(HealthContributorRegistry healthContributorRegistry,
				ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry,
				HealthCheckedGrpcComponents healthCheckedGrpcComponents) {
			return new GrpcServerHealth(healthContributorRegistry, reactiveHealthContributorRegistry.getIfAvailable(),
					healthCheckedGrpcComponents);
		}

		@Bean
		@ConditionalOnBooleanProperty(name = VALIDATE_MEMBERSHIP_PROPERTY, matchIfMissing = true)
		HealthContributorMembershipValidator grpcServerHealthServiceMembershipValidator(
				GrpcServerHealthProperties properties, HealthContributorRegistry healthContributorRegistry,
				ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry) {
			return new HealthContributorMembershipValidator(healthContributorRegistry,
					reactiveHealthContributorRegistry.getIfAvailable(), VALIDATE_MEMBERSHIP_PROPERTY,
					(members) -> properties.getService().forEach((serviceName, service) -> {
						String property = "spring.grpc.server.health.service." + serviceName;
						members.member(property + ".include", service.getInclude());
						members.member(property + ".exclude", service.getExclude());
					}));
		}

	}

	static class NotDisabledAndHasBindableServiceOrExplicitlyEnabledCondition extends AnyNestedCondition {

		NotDisabledAndHasBindableServiceOrExplicitlyEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(BindableService.class)
		@ConditionalOnBooleanProperty(name = "spring.grpc.server.health.enabled", matchIfMissing = true)
		static class NotDisabledAndHasBindableService {

		}

		@ConditionalOnBooleanProperty(name = "spring.grpc.server.health.enabled")
		static class ExplicitlyEnabled {

		}

	}

}
