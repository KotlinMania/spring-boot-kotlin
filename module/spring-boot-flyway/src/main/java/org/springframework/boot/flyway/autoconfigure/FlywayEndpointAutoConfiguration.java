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

package io.github.kotlinmania.spring.boot.flyway.autoconfigure;

import org.flywaydb.core.Flyway;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.flyway.actuate.endpoint.FlywayEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link FlywayEndpoint}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = FlywayAutoConfiguration.class)
@ConditionalOnClass({ Flyway.class, ConditionalOnAvailableEndpoint.class })
@ConditionalOnAvailableEndpoint(FlywayEndpoint.class)
public final class FlywayEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(Flyway.class)
	@ConditionalOnMissingBean
	FlywayEndpoint flywayEndpoint(ApplicationContext context) {
		return new FlywayEndpoint(context);
	}

}
