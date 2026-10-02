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

package io.github.kotlinmania.spring.boot.actuate.autoconfigure.startup;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.startup.StartupEndpoint;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionMessage;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionOutcome;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.SpringBootCondition;
import io.github.kotlinmania.spring.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link StartupEndpoint}.
 *
 * @author Brian Clozel
 * @since 2.4.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(StartupEndpoint.class)
@Conditional(StartupEndpointAutoConfiguration.ApplicationStartupCondition.class)
public final class StartupEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	StartupEndpoint startupEndpoint(BufferingApplicationStartup applicationStartup) {
		return new StartupEndpoint(applicationStartup);
	}

	/**
	 * {@link SpringBootCondition} checking the configured
	 * {@link org.springframework.core.metrics.ApplicationStartup}.
	 * <p>
	 * Endpoint is enabled only if the configured implementation is
	 * {@link BufferingApplicationStartup}.
	 */
	static class ApplicationStartupCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("ApplicationStartup");
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			Assert.state(beanFactory != null, "'beanFactory' must not be null");
			ApplicationStartup applicationStartup = beanFactory.getApplicationStartup();
			if (applicationStartup instanceof BufferingApplicationStartup) {
				return ConditionOutcome
					.match(message.because("configured applicationStartup is of type BufferingApplicationStartup."));
			}
			return ConditionOutcome.noMatch(message.because("configured applicationStartup is of type "
					+ applicationStartup.getClass() + ", expected BufferingApplicationStartup."));
		}

	}

}
