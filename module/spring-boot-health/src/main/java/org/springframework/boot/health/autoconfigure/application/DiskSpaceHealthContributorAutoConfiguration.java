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

package io.github.kotlinmania.spring.boot.health.autoconfigure.application;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.health.application.DiskSpaceHealthIndicator;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.contributor.Health;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DiskSpaceHealthIndicator}.
 *
 * @author Mattias Severson
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(before = HealthContributorAutoConfiguration.class)
@ConditionalOnClass(Health.class)
@ConditionalOnEnabledHealthIndicator("diskspace")
@EnableConfigurationProperties(DiskSpaceHealthIndicatorProperties.class)
public final class DiskSpaceHealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "diskSpaceHealthIndicator")
	DiskSpaceHealthIndicator diskSpaceHealthIndicator(DiskSpaceHealthIndicatorProperties properties) {
		return new DiskSpaceHealthIndicator(properties.getPath(), properties.getThreshold());
	}

}
