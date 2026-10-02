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

package io.github.kotlinmania.spring.boot.webservices.autoconfigure.client;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.http.client.ClientHttpRequestFactoryBuilder;
import io.github.kotlinmania.spring.boot.http.client.HttpClientSettings;
import io.github.kotlinmania.spring.boot.webservices.client.WebServiceMessageSenderFactory;
import io.github.kotlinmania.spring.boot.webservices.client.WebServiceTemplateBuilder;
import io.github.kotlinmania.spring.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebServiceTemplate}.
 *
 * @author Dmytro Nosan
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ WebServiceTemplate.class, Unmarshaller.class, Marshaller.class })
public final class WebServiceTemplateAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	WebServiceMessageSenderFactory webServiceHttpMessageSenderFactory(
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
			ObjectProvider<HttpClientSettings> httpClientSettings) {
		return WebServiceMessageSenderFactory.http(
				clientHttpRequestFactoryBuilder.getIfAvailable(ClientHttpRequestFactoryBuilder::detect),
				httpClientSettings.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean
	WebServiceTemplateBuilder webServiceTemplateBuilder(
			ObjectProvider<WebServiceMessageSenderFactory> httpWebServiceMessageSenderBuilder,
			ObjectProvider<WebServiceTemplateCustomizer> webServiceTemplateCustomizers) {
		WebServiceTemplateBuilder templateBuilder = new WebServiceTemplateBuilder();
		WebServiceMessageSenderFactory httpMessageSenderFactory = httpWebServiceMessageSenderBuilder.getIfAvailable();
		if (httpMessageSenderFactory != null) {
			templateBuilder = templateBuilder.httpMessageSenderFactory(httpMessageSenderFactory);
		}
		List<WebServiceTemplateCustomizer> customizers = webServiceTemplateCustomizers.orderedStream().toList();
		if (!customizers.isEmpty()) {
			templateBuilder = templateBuilder.customizers(customizers);
		}
		return templateBuilder;
	}

}
