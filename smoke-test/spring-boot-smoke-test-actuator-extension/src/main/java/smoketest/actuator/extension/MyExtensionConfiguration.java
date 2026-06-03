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

package smoketest.actuator.extension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import io.github.kotlinmania.spring.boot.actuate.endpoint.EndpointFilter;
import io.github.kotlinmania.spring.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import io.github.kotlinmania.spring.boot.actuate.endpoint.invoke.ParameterValueMapper;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointMediaTypes;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.ExposableWebEndpoint;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebEndpointsSupplier;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;

@Configuration(proxyBeanMethods = false)
public class MyExtensionConfiguration {

	@Bean
	public MyExtensionWebMvcEndpointHandlerMapping myWebMvcEndpointHandlerMapping(
			WebEndpointsSupplier webEndpointsSupplier, EndpointMediaTypes endpointMediaTypes,
			ObjectProvider<CorsEndpointProperties> corsPropertiesProvider, WebEndpointProperties webEndpointProperties,
			Environment environment, ApplicationContext applicationContext, ParameterValueMapper parameterMapper) {
		CorsEndpointProperties corsProperties = corsPropertiesProvider.getIfAvailable();
		CorsConfiguration corsConfiguration = (corsProperties != null) ? corsProperties.toCorsConfiguration() : null;
		List<OperationInvokerAdvisor> invokerAdvisors = Collections.emptyList();
		List<EndpointFilter<ExposableWebEndpoint>> filters = Collections
			.singletonList(new MyExtensionEndpointFilter(environment));
		WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(applicationContext, parameterMapper,
				endpointMediaTypes, null, null, invokerAdvisors, filters, Collections.emptyList());
		Collection<ExposableWebEndpoint> endpoints = discoverer.getEndpoints();
		return new MyExtensionWebMvcEndpointHandlerMapping(endpoints, endpointMediaTypes, corsConfiguration);
	}

}
