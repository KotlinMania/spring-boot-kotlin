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

package io.github.kotlinmania.spring.boot.couchbase.autoconfigure.health;

import com.couchbase.client.java.Cluster;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration;
import io.github.kotlinmania.spring.boot.couchbase.health.CouchbaseReactiveHealthIndicator;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.CompositeReactiveHealthContributorConfiguration;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import io.github.kotlinmania.spring.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CouchbaseReactiveHealthIndicator}.
 *
 * @author Mikalai Lushchytski
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = CouchbaseAutoConfiguration.class)
@ConditionalOnClass({ Cluster.class, Flux.class, CouchbaseReactiveHealthIndicator.class,
		ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(Cluster.class)
@ConditionalOnEnabledHealthIndicator("couchbase")
public final class CouchbaseReactiveHealthContributorAutoConfiguration
		extends CompositeReactiveHealthContributorConfiguration<CouchbaseReactiveHealthIndicator, Cluster> {

	CouchbaseReactiveHealthContributorAutoConfiguration() {
		super(CouchbaseReactiveHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "couchbaseHealthIndicator", "couchbaseHealthContributor" })
	ReactiveHealthContributor couchbaseHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, Cluster.class);
	}

}
