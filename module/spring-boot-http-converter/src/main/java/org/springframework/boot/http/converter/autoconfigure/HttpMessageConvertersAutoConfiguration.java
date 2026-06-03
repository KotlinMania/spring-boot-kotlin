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

package io.github.kotlinmania.spring.boot.http.converter.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.NoneNestedConditions;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration.NotReactiveWebApplicationCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpMessageConverter}s.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Piotr Maj
 * @author Oliver Gierke
 * @author David Liu
 * @author Andy Wilkinson
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Dmitry Sulman
 * @author Brian Clozel
 * @since 4.0.0
 */
@SuppressWarnings("removal")
@AutoConfiguration(afterName = { "io.github.kotlinmania.spring.boot.jackson.autoconfigure.JacksonAutoConfiguration",
		"io.github.kotlinmania.spring.boot.jackson2.autoconfigure.Jackson2AutoConfiguration",
		"io.github.kotlinmania.spring.boot.jsonb.autoconfigure.JsonbAutoConfiguration",
		"io.github.kotlinmania.spring.boot.gson.autoconfigure.GsonAutoConfiguration",
		"io.github.kotlinmania.spring.boot.kotlinx.serialization.json.autoconfigure.KotlinxSerializationJsonAutoConfiguration" })
@ConditionalOnClass(HttpMessageConverter.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@Import({ JacksonHttpMessageConvertersConfiguration.class, Jackson2HttpMessageConvertersConfiguration.class,
		GsonHttpMessageConvertersConfiguration.class, JsonbHttpMessageConvertersConfiguration.class,
		KotlinSerializationHttpMessageConvertersConfiguration.class })
public final class HttpMessageConvertersAutoConfiguration {

	static final String PREFERRED_MAPPER_PROPERTY = "spring.http.converters.preferred-json-mapper";

	@Bean
	@Order(0)
	@SuppressWarnings("deprecation")
	ClientHttpMessageConvertersCustomizer clientConvertersCustomizer(
			ObjectProvider<HttpMessageConverters> legacyConverters,
			ObjectProvider<HttpMessageConverter<?>> converters) {
		return new DefaultClientHttpMessageConvertersCustomizer(legacyConverters.getIfAvailable(),
				converters.orderedStream().toList());
	}

	@Bean
	@Order(0)
	@SuppressWarnings("deprecation")
	ServerHttpMessageConvertersCustomizer serverConvertersCustomizer(
			ObjectProvider<HttpMessageConverters> legacyConverters,
			ObjectProvider<HttpMessageConverter<?>> converters) {
		return new DefaultServerHttpMessageConvertersCustomizer(legacyConverters.getIfAvailable(),
				converters.orderedStream().toList());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(HttpMessageConvertersProperties.class)
	protected static class StringHttpMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean(StringHttpMessageConverter.class)
		StringHttpMessageConvertersCustomizer stringHttpMessageConvertersCustomizer(
				HttpMessageConvertersProperties properties) {
			return new StringHttpMessageConvertersCustomizer(properties);
		}

	}

	static class StringHttpMessageConvertersCustomizer
			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

		StringHttpMessageConverter converter;

		StringHttpMessageConvertersCustomizer(HttpMessageConvertersProperties properties) {
			this.converter = new StringHttpMessageConverter(properties.getStringEncodingCharset());
			this.converter.setWriteAcceptCharset(false);
		}

		@Override
		public void customize(ClientBuilder builder) {
			builder.withStringConverter(this.converter);
		}

		@Override
		public void customize(ServerBuilder builder) {
			builder.withStringConverter(this.converter);
		}

	}

	static class NotReactiveWebApplicationCondition extends NoneNestedConditions {

		NotReactiveWebApplicationCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnWebApplication(type = Type.REACTIVE)
		private static final class ReactiveWebApplication {

		}

	}

}
