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

package io.github.kotlinmania.spring.boot.data.redis.autoconfigure.health;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import io.github.kotlinmania.spring.boot.data.redis.health.DataRedisHealthIndicator;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import io.github.kotlinmania.spring.boot.health.contributor.HealthContributor;
import io.github.kotlinmania.spring.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DataRedisHealthIndicator}.
 *
 * @author Christian Dupuis
 * @author Richard Santana
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @since 4.0.0
 */
@AutoConfiguration(
		after = { DataRedisAutoConfiguration.class, DataRedisReactiveHealthContributorAutoConfiguration.class })
@ConditionalOnClass({ RedisConnectionFactory.class, HealthIndicator.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnEnabledHealthIndicator("redis")
public final class DataRedisHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<DataRedisHealthIndicator, RedisConnectionFactory> {

	DataRedisHealthContributorAutoConfiguration() {
		super(DataRedisHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "redisHealthIndicator", "redisHealthContributor" })
	HealthContributor redisHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, RedisConnectionFactory.class);
	}

}
