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

package io.github.kotlinmania.spring.boot.data.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.data.autoconfigure.metrics.DataMetricsProperties.Repository;
import io.github.kotlinmania.spring.boot.data.metrics.DefaultRepositoryTagsProvider;
import io.github.kotlinmania.spring.boot.data.metrics.MetricsRepositoryMethodInvocationListener;
import io.github.kotlinmania.spring.boot.data.metrics.RepositoryTagsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Repository metrics.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(afterName = {
		"io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
		"io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
		"io.github.kotlinmania.spring.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration" })
@ConditionalOnClass(org.springframework.data.repository.Repository.class)
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties(DataMetricsProperties.class)
public final class DataRepositoryMetricsAutoConfiguration {

	private final DataMetricsProperties properties;

	DataRepositoryMetricsAutoConfiguration(DataMetricsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(RepositoryTagsProvider.class)
	DefaultRepositoryTagsProvider repositoryTagsProvider() {
		return new DefaultRepositoryTagsProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	MetricsRepositoryMethodInvocationListener metricsRepositoryMethodInvocationListener(
			ObjectProvider<MeterRegistry> registry, RepositoryTagsProvider tagsProvider) {
		Repository properties = this.properties.getRepository();
		return new MetricsRepositoryMethodInvocationListener(registry::getObject, tagsProvider,
				properties.getMetricName(), new PropertiesAutoTimer(properties.getAutotime()));
	}

	@Bean
	static MetricsRepositoryMethodInvocationListenerBeanPostProcessor metricsRepositoryMethodInvocationListenerBeanPostProcessor(
			ObjectProvider<MetricsRepositoryMethodInvocationListener> metricsRepositoryMethodInvocationListener) {
		return new MetricsRepositoryMethodInvocationListenerBeanPostProcessor(
				SingletonSupplier.of(metricsRepositoryMethodInvocationListener::getObject));
	}

}
