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

package io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.export.jmx;

import io.micrometer.core.instrument.Clock;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.export.ConditionalOnEnabledMetricsExport;
import io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to JMX.
 *
 * @author Jon Schneider
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(JmxMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("jmx")
@EnableConfigurationProperties(JmxProperties.class)
public final class JmxMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	JmxConfig jmxConfig(JmxProperties jmxProperties) {
		return new JmxPropertiesConfigAdapter(jmxProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	JmxMeterRegistry jmxMeterRegistry(JmxConfig jmxConfig, Clock clock) {
		return new JmxMeterRegistry(jmxConfig, clock);
	}

}
