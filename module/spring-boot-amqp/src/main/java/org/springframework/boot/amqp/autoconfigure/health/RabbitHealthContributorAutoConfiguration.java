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

package io.github.kotlinmania.spring.boot.amqp.autoconfigure.health;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import io.github.kotlinmania.spring.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import io.github.kotlinmania.spring.boot.amqp.health.RabbitHealthIndicator;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import io.github.kotlinmania.spring.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RabbitHealthIndicator}.
 *
 * @author Christian Dupuis
 * @since 4.0.0
 */
@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass({ RabbitHealthIndicator.class, RabbitTemplate.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(RabbitTemplate.class)
@ConditionalOnEnabledHealthIndicator("rabbit")
public final class RabbitHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<RabbitHealthIndicator, RabbitTemplate> {

	RabbitHealthContributorAutoConfiguration() {
		super(RabbitHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "rabbitHealthIndicator", "rabbitHealthContributor" })
	HealthContributor rabbitHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, RabbitTemplate.class);
	}

}
