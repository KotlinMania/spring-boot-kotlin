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
import io.github.kotlinmania.spring.boot.actuate.logging.LogFileWebEndpoint;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionMessage;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionOutcome;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.SpringBootCondition;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.logging.LogFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link LogFileWebEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Christian Carriere-Tisseur
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(LogFileWebEndpoint.class)
@EnableConfigurationProperties(LogFileWebEndpointProperties.class)
public final class LogFileWebEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(LogFileCondition.class)
	LogFileWebEndpoint logFileWebEndpoint(ObjectProvider<LogFile> logFile, LogFileWebEndpointProperties properties) {
		return new LogFileWebEndpoint(logFile.getIfAvailable(), properties.getExternalFile());
	}

	private static final class LogFileCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			String config = getLogFileConfig(environment, LogFile.FILE_NAME_PROPERTY);
			ConditionMessage.Builder message = ConditionMessage.forCondition("Log File");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match(message.found(LogFile.FILE_NAME_PROPERTY).items(config));
			}
			config = getLogFileConfig(environment, LogFile.FILE_PATH_PROPERTY);
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match(message.found(LogFile.FILE_PATH_PROPERTY).items(config));
			}
			config = environment.getProperty("management.endpoint.logfile.external-file");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match(message.found("management.endpoint.logfile.external-file").items(config));
			}
			return ConditionOutcome.noMatch(message.didNotFind("logging file").atAll());
		}

		private String getLogFileConfig(Environment environment, String configName) {
			return environment.resolvePlaceholders("${" + configName + ":}");
		}

	}

}
