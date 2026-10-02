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

package io.github.kotlinmania.spring.boot.cassandra.autoconfigure.health;

import com.datastax.oss.driver.api.core.CqlSession;
import reactor.core.publisher.Flux;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.cassandra.autoconfigure.CassandraAutoConfiguration;
import io.github.kotlinmania.spring.boot.cassandra.autoconfigure.health.CassandraHealthContributorConfigurations.CassandraReactiveDriverConfiguration;
import io.github.kotlinmania.spring.boot.cassandra.health.CassandraDriverReactiveHealthIndicator;
import io.github.kotlinmania.spring.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CassandraDriverReactiveHealthIndicator}.
 *
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = CassandraAutoConfiguration.class)
@ConditionalOnClass({ CqlSession.class, Flux.class, CassandraDriverReactiveHealthIndicator.class,
		ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnEnabledHealthIndicator("cassandra")
@Import(CassandraReactiveDriverConfiguration.class)
public final class CassandraReactiveHealthContributorAutoConfiguration {

}
