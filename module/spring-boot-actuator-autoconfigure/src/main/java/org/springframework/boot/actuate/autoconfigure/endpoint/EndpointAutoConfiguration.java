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

package io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.actuate.endpoint.EndpointAccessResolver;
import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.Endpoint;
import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.EndpointConverter;
import io.github.kotlinmania.spring.boot.actuate.endpoint.invoke.ParameterValueMapper;
import io.github.kotlinmania.spring.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import io.github.kotlinmania.spring.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link Endpoint @Endpoint}
 * support.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Chao Chang
 * @since 2.0.0
 */
@AutoConfiguration
public final class EndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ParameterValueMapper endpointOperationParameterMapper(@EndpointConverter ObjectProvider<Converter<?, ?>> converters,
			@EndpointConverter ObjectProvider<GenericConverter> genericConverters) {
		ConversionService conversionService = createConversionService(converters.orderedStream().toList(),
				genericConverters.orderedStream().toList());
		return new ConversionServiceParameterValueMapper(conversionService);
	}

	private ConversionService createConversionService(List<Converter<?, ?>> converters,
			List<GenericConverter> genericConverters) {
		if (genericConverters.isEmpty() && converters.isEmpty()) {
			return ApplicationConversionService.getSharedInstance();
		}
		ApplicationConversionService conversionService = new ApplicationConversionService();
		converters.forEach(conversionService::addConverter);
		genericConverters.forEach(conversionService::addConverter);
		return conversionService;
	}

	@Bean
	@ConditionalOnMissingBean
	CachingOperationInvokerAdvisor endpointCachingOperationInvokerAdvisor(Environment environment) {
		return new CachingOperationInvokerAdvisor(new EndpointIdTimeToLivePropertyFunction(environment));
	}

	@Bean
	@ConditionalOnMissingBean(EndpointAccessResolver.class)
	PropertiesEndpointAccessResolver propertiesEndpointAccessResolver(Environment environment) {
		return new PropertiesEndpointAccessResolver(environment);
	}

}
