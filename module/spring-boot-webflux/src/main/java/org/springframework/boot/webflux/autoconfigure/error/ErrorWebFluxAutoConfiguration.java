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

package io.github.kotlinmania.spring.boot.webflux.autoconfigure.error;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.SearchStrategy;
import io.github.kotlinmania.spring.boot.autoconfigure.web.WebProperties;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import io.github.kotlinmania.spring.boot.webflux.error.DefaultErrorAttributes;
import io.github.kotlinmania.spring.boot.webflux.error.ErrorAttributes;
import io.github.kotlinmania.spring.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to render errors through a WebFlux
 * {@link org.springframework.web.server.WebExceptionHandler}.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 4.0.0
 */
@AutoConfiguration(before = WebFluxAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass(WebFluxConfigurer.class)
@EnableConfigurationProperties(WebProperties.class)
public final class ErrorWebFluxAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	@Order(-1)
	ErrorWebExceptionHandler errorWebExceptionHandler(ErrorAttributes errorAttributes, WebProperties webProperties,
			ObjectProvider<ViewResolver> viewResolvers, ServerCodecConfigurer serverCodecConfigurer,
			ApplicationContext applicationContext) {
		DefaultErrorWebExceptionHandler exceptionHandler = new DefaultErrorWebExceptionHandler(errorAttributes,
				webProperties.getResources(), webProperties.getError(), applicationContext);
		exceptionHandler.setViewResolvers(viewResolvers.orderedStream().toList());
		exceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
		exceptionHandler.setMessageReaders(serverCodecConfigurer.getReaders());
		return exceptionHandler;
	}

	@Bean
	@ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
	DefaultErrorAttributes errorAttributes() {
		return new DefaultErrorAttributes();
	}

}
