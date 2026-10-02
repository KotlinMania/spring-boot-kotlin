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

package io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionMessage;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionOutcome;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.SpringBootCondition;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.logging.logback.LogbackMetricsAutoConfiguration.LogbackLoggingCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Logback metrics.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass({ MeterRegistry.class, LoggerContext.class, LoggerFactory.class })
@ConditionalOnBean(MeterRegistry.class)
@Conditional(LogbackLoggingCondition.class)
public final class LogbackMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	LogbackMetrics logbackMetrics() {
		return new LogbackMetrics();
	}

	static class LogbackLoggingCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
			ConditionMessage.Builder message = ConditionMessage.forCondition("LogbackLoggingCondition");
			if (loggerFactory instanceof LoggerContext) {
				return ConditionOutcome.match(message.because("ILoggerFactory is a Logback LoggerContext"));
			}
			return ConditionOutcome.noMatch(
					message.because("ILoggerFactory is an instance of " + loggerFactory.getClass().getCanonicalName()));
		}

	}

}
