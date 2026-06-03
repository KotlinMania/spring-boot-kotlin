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

package io.github.kotlinmania.spring.boot.health.autoconfigure.registry;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.health.contributor.HealthContributor;
import io.github.kotlinmania.spring.boot.health.contributor.ReactiveHealthContributor;
import io.github.kotlinmania.spring.boot.health.registry.DefaultHealthContributorRegistry;
import io.github.kotlinmania.spring.boot.health.registry.DefaultReactiveHealthContributorRegistry;
import io.github.kotlinmania.spring.boot.health.registry.HealthContributorNameValidator;
import io.github.kotlinmania.spring.boot.health.registry.HealthContributorRegistry;
import io.github.kotlinmania.spring.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link HealthContributorRegistry} and {@link ReactiveHealthContributorRegistry}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
public final class HealthContributorRegistryAutoConfiguration {

	HealthContributorRegistryAutoConfiguration() {
	}

	@Bean
	@ConditionalOnMissingBean(HealthContributorRegistry.class)
	DefaultHealthContributorRegistry healthContributorRegistry(Map<String, HealthContributor> contributorBeans,
			ObjectProvider<HealthContributorNameGenerator> nameGeneratorProvider,
			List<HealthContributorNameValidator> nameValidators) {
		HealthContributorNameGenerator nameGenerator = nameGeneratorProvider
			.getIfAvailable(HealthContributorNameGenerator::withoutStandardSuffixes);
		return new DefaultHealthContributorRegistry(nameValidators, nameGenerator.registrar(contributorBeans));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Flux.class)
	static class ReactiveHealthContributorRegistryConfiguration {

		@Bean
		@ConditionalOnMissingBean(ReactiveHealthContributorRegistry.class)
		DefaultReactiveHealthContributorRegistry reactiveHealthContributorRegistry(
				Map<String, ReactiveHealthContributor> contributorBeans,
				ObjectProvider<HealthContributorNameGenerator> nameGeneratorProvider,
				List<HealthContributorNameValidator> nameValidators) {
			HealthContributorNameGenerator nameGenerator = nameGeneratorProvider
				.getIfAvailable(HealthContributorNameGenerator::withoutStandardSuffixes);
			return new DefaultReactiveHealthContributorRegistry(nameValidators,
					nameGenerator.registrar(contributorBeans));
		}

	}

}
