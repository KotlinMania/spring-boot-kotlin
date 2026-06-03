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

package io.github.kotlinmania.spring.boot.http.client.autoconfigure.imperative;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.http.client.ClientHttpRequestFactoryBuilder;
import io.github.kotlinmania.spring.boot.http.client.HttpClientSettings;
import io.github.kotlinmania.spring.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import io.github.kotlinmania.spring.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer;
import io.github.kotlinmania.spring.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import io.github.kotlinmania.spring.boot.thread.Threading;
import io.github.kotlinmania.spring.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for imperative HTTP clients.
 *
 * @author Phillip Webb
 * @author Sangmin Park
 * @since 4.0.0
 * @see HttpClientAutoConfiguration
 */
@AutoConfiguration(after = HttpClientAutoConfiguration.class)
@ConditionalOnClass(ClientHttpRequestFactory.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@EnableConfigurationProperties(ImperativeHttpClientsProperties.class)
public final class ImperativeHttpClientAutoConfiguration {

	private final Environment environment;

	ImperativeHttpClientAutoConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Bean
	@ConditionalOnMissingBean
	ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder(ResourceLoader resourceLoader,
			ImperativeHttpClientsProperties properties,
			ObjectProvider<ClientHttpRequestFactoryBuilderCustomizer<?>> clientHttpRequestFactoryBuilderCustomizers) {
		ClientHttpRequestFactoryBuilder<?> builder = (properties.getFactory() != null)
				? properties.getFactory().builder()
				: ClientHttpRequestFactoryBuilder.detect(resourceLoader.getClassLoader());
		if (builder instanceof JdkClientHttpRequestFactoryBuilder jdk && Threading.VIRTUAL.isActive(this.environment)) {
			builder = jdk.withExecutor(new VirtualThreadTaskExecutor("httpclient-"));
		}
		return customize(builder, clientHttpRequestFactoryBuilderCustomizers.orderedStream().toList());
	}

	@SuppressWarnings("unchecked")
	private ClientHttpRequestFactoryBuilder<?> customize(ClientHttpRequestFactoryBuilder<?> builder,
			List<ClientHttpRequestFactoryBuilderCustomizer<?>> customizers) {
		ClientHttpRequestFactoryBuilder<?>[] builderReference = { builder };
		LambdaSafe.callbacks(ClientHttpRequestFactoryBuilderCustomizer.class, customizers, builderReference[0])
			.invoke((customizer) -> builderReference[0] = customizer.customize(builderReference[0]));
		return builderReference[0];
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	ClientHttpRequestFactory clientHttpRequestFactory(
			ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder, HttpClientSettings httpClientSettings) {
		return clientHttpRequestFactoryBuilder.build(httpClientSettings);
	}

}
