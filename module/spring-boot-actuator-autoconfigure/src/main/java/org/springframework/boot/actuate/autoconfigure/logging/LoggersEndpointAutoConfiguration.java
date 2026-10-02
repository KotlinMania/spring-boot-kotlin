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

package io.github.kotlinmania.spring.boot.actuate.autoconfigure.logging;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.logging.LoggersEndpoint;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionMessage;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionOutcome;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.SpringBootCondition;
import io.github.kotlinmania.spring.boot.logging.LoggerGroups;
import io.github.kotlinmania.spring.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link LoggersEndpoint}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(LoggersEndpoint.class)
public final class LoggersEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(LoggingSystem.class)
	@Conditional(OnEnabledLoggingSystemCondition.class)
	@ConditionalOnMissingBean
	LoggersEndpoint loggersEndpoint(LoggingSystem loggingSystem, ObjectProvider<LoggerGroups> springBootLoggerGroups) {
		return new LoggersEndpoint(loggingSystem, springBootLoggerGroups.getIfAvailable(LoggerGroups::new));
	}

	static class OnEnabledLoggingSystemCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("Logging System");
			String loggingSystem = System.getProperty(LoggingSystem.SYSTEM_PROPERTY);
			if (LoggingSystem.NONE.equals(loggingSystem)) {
				return ConditionOutcome
					.noMatch(message.because("system property " + LoggingSystem.SYSTEM_PROPERTY + " is set to none"));
			}
			return ConditionOutcome.match(message.because("enabled"));
		}

	}

}
