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

package io.github.kotlinmania.spring.boot.webclient.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.http.client.HttpClientSettings;
import io.github.kotlinmania.spring.boot.http.client.autoconfigure.reactive.ReactiveHttpClientAutoConfiguration;
import io.github.kotlinmania.spring.boot.http.client.reactive.ClientHttpConnectorBuilder;
import io.github.kotlinmania.spring.boot.http.codec.CodecCustomizer;
import io.github.kotlinmania.spring.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import io.github.kotlinmania.spring.boot.ssl.SslBundles;
import io.github.kotlinmania.spring.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebClient}.
 * <p>
 * This will produce a
 * {@link org.springframework.web.reactive.function.client.WebClient.Builder
 * WebClient.Builder} bean with the {@code prototype} scope, meaning each injection point
 * will receive a newly cloned instance of the builder.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = { ReactiveHttpClientAutoConfiguration.class, CodecsAutoConfiguration.class })
@ConditionalOnClass(WebClient.class)
public final class WebClientAutoConfiguration {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean
	WebClient.Builder webClientBuilder(ObjectProvider<WebClientCustomizer> customizerProvider) {
		WebClient.Builder builder = WebClient.builder();
		customizerProvider.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder;
	}

	@Bean
	@Lazy
	@Order(0)
	@ConditionalOnBean(ClientHttpConnector.class)
	WebClientCustomizer webClientHttpConnectorCustomizer(ClientHttpConnector clientHttpConnector) {
		return (builder) -> builder.clientConnector(clientHttpConnector);
	}

	@Bean
	@ConditionalOnMissingBean(WebClientSsl.class)
	@ConditionalOnBean(SslBundles.class)
	AutoConfiguredWebClientSsl webClientSsl(ResourceLoader resourceLoader,
			ObjectProvider<ClientHttpConnectorBuilder<?>> clientHttpConnectorBuilder,
			ObjectProvider<HttpClientSettings> httpClientSettings, SslBundles sslBundles) {
		return new AutoConfiguredWebClientSsl(
				clientHttpConnectorBuilder
					.getIfAvailable(() -> ClientHttpConnectorBuilder.detect(resourceLoader.getClassLoader())),
				httpClientSettings.getIfAvailable(HttpClientSettings::defaults), sslBundles);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(CodecCustomizer.class)
	protected static class WebClientCodecsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@Order(0)
		WebClientCodecCustomizer exchangeStrategiesCustomizer(ObjectProvider<CodecCustomizer> codecCustomizers) {
			return new WebClientCodecCustomizer(codecCustomizers.orderedStream().toList());
		}

	}

}
