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

package io.github.kotlinmania.spring.boot.restclient.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.http.client.ClientHttpRequestFactoryBuilder;
import io.github.kotlinmania.spring.boot.http.client.HttpClientSettings;
import io.github.kotlinmania.spring.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import io.github.kotlinmania.spring.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import io.github.kotlinmania.spring.boot.restclient.RestTemplateBuilder;
import io.github.kotlinmania.spring.boot.restclient.RestTemplateCustomizer;
import io.github.kotlinmania.spring.boot.restclient.RestTemplateRequestCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.client.RestTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RestTemplate} (via
 * {@link RestTemplateBuilder}).
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = HttpClientAutoConfiguration.class)
@ConditionalOnClass({ RestTemplate.class, HttpMessageConverters.class })
@Conditional(NotReactiveWebApplicationCondition.class)
public final class RestTemplateAutoConfiguration {

	@Bean
	@Lazy
	RestTemplateBuilderConfigurer restTemplateBuilderConfigurer(
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
			ObjectProvider<HttpClientSettings> httpClientSettings,
			ObjectProvider<ClientHttpMessageConvertersCustomizer> convertersCustomizers,
			ObjectProvider<RestTemplateCustomizer> restTemplateCustomizers,
			ObjectProvider<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {
		RestTemplateBuilderConfigurer configurer = new RestTemplateBuilderConfigurer();
		configurer.setRequestFactoryBuilder(clientHttpRequestFactoryBuilder.getIfAvailable());
		configurer.setClientSettings(httpClientSettings.getIfAvailable());
		configurer.setHttpMessageConvertersCustomizers(convertersCustomizers.orderedStream().toList());
		configurer.setRestTemplateCustomizers(restTemplateCustomizers.orderedStream().toList());
		configurer.setRestTemplateRequestCustomizers(restTemplateRequestCustomizers.orderedStream().toList());
		return configurer;
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer restTemplateBuilderConfigurer) {
		return restTemplateBuilderConfigurer.configure(new RestTemplateBuilder());
	}

}
