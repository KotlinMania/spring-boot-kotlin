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

package io.github.kotlinmania.spring.boot.mongodb.autoconfigure.health;

import com.mongodb.client.MongoClient;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import io.github.kotlinmania.spring.boot.health.contributor.HealthContributor;
import io.github.kotlinmania.spring.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import io.github.kotlinmania.spring.boot.mongodb.health.MongoHealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MongoHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = { MongoReactiveHealthContributorAutoConfiguration.class, MongoAutoConfiguration.class })
@ConditionalOnClass({ MongoClient.class, MongoHealthIndicator.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(MongoClient.class)
@ConditionalOnEnabledHealthIndicator("mongodb")
public final class MongoHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<MongoHealthIndicator, MongoClient> {

	MongoHealthContributorAutoConfiguration() {
		super(MongoHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "mongoHealthIndicator", "mongoHealthContributor" })
	HealthContributor mongoHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, MongoClient.class);
	}

}
