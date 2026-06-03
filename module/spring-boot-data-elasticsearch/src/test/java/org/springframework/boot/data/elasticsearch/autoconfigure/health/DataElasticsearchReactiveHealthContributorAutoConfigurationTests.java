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

package io.github.kotlinmania.spring.boot.data.elasticsearch.autoconfigure.health;

import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;
import io.github.kotlinmania.spring.boot.data.elasticsearch.health.DataElasticsearchReactiveHealthIndicator;
import io.github.kotlinmania.spring.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import io.github.kotlinmania.spring.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration;
import io.github.kotlinmania.spring.boot.elasticsearch.autoconfigure.health.ElasticsearchRestHealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.elasticsearch.health.ElasticsearchRestClientHealthIndicator;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataElasticsearchReactiveHealthContributorAutoConfiguration}.
 *
 * @author Aleksander Lech
 */
class DataElasticsearchReactiveHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataElasticsearchAutoConfiguration.class,
				ElasticsearchClientAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class,
				DataElasticsearchReactiveHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(DataElasticsearchReactiveHealthIndicator.class)
				.hasBean("elasticsearchHealthContributor"));
	}

	@Test
	void runWithRegularIndicatorShouldOnlyCreateReactiveIndicator() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(ElasticsearchRestHealthContributorAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(DataElasticsearchReactiveHealthIndicator.class)
				.hasBean("elasticsearchHealthContributor")
				.doesNotHaveBean(ElasticsearchRestClientHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.elasticsearch.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(DataElasticsearchReactiveHealthIndicator.class)
				.doesNotHaveBean("elasticsearchHealthContributor"));
	}

}
