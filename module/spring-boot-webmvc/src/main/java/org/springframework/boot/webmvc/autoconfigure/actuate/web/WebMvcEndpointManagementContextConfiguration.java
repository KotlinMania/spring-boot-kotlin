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

package io.github.kotlinmania.spring.boot.webmvc.autoconfigure.actuate.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.config.BeanDefinition;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ManagementPortType;
import io.github.kotlinmania.spring.boot.actuate.endpoint.EndpointAccessResolver;
import io.github.kotlinmania.spring.boot.actuate.endpoint.ExposableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.endpoint.OperationResponseBody;
import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.Endpoint;
import io.github.kotlinmania.spring.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointLinksResolver;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointMapping;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointMediaTypes;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.ExposableWebEndpoint;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebEndpointsSupplier;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebServerNamespace;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpoint;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpointGroups;
import io.github.kotlinmania.spring.boot.webmvc.actuate.endpoint.web.AdditionalHealthEndpointPathsWebMvcHandlerMapping;
import io.github.kotlinmania.spring.boot.webmvc.actuate.endpoint.web.WebMvcEndpointHandlerMapping;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.DispatcherServletPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Spring MVC
 * {@link Endpoint @Endpoint} concerns.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnBean({ DispatcherServlet.class, WebEndpointsSupplier.class })
@EnableConfigurationProperties(CorsEndpointProperties.class)
public class WebMvcEndpointManagementContextConfiguration {

	private static final List<MediaType> MEDIA_TYPES = Collections
		.unmodifiableList(Arrays.asList(MediaType.APPLICATION_JSON, new MediaType("application", "*+json")));

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings("removal")
	WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
			io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier servletEndpointsSupplier,
			io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier controllerEndpointsSupplier,
			EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
			WebEndpointProperties webEndpointProperties, Environment environment) {
		List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		allEndpoints.addAll(webEndpoints);
		allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
		allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
		String basePath = webEndpointProperties.getBasePath();
		EndpointMapping endpointMapping = new EndpointMapping(basePath);
		boolean shouldRegisterLinksMapping = shouldRegisterLinksMapping(webEndpointProperties, environment, basePath);
		return new WebMvcEndpointHandlerMapping(endpointMapping, webEndpoints, endpointMediaTypes,
				corsProperties.toCorsConfiguration(), new EndpointLinksResolver(allEndpoints, basePath),
				shouldRegisterLinksMapping);
	}

	private boolean shouldRegisterLinksMapping(WebEndpointProperties webEndpointProperties, Environment environment,
			String basePath) {
		return webEndpointProperties.getDiscovery().isEnabled() && (StringUtils.hasText(basePath)
				|| ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
	}

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings("removal")
	@Deprecated(since = "3.3.5", forRemoval = true)
	io.github.kotlinmania.spring.boot.webmvc.actuate.endpoint.web.ControllerEndpointHandlerMapping controllerEndpointHandlerMapping(
			io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier controllerEndpointsSupplier,
			CorsEndpointProperties corsProperties, WebEndpointProperties webEndpointProperties,
			EndpointAccessResolver endpointAccessResolver) {
		EndpointMapping endpointMapping = new EndpointMapping(webEndpointProperties.getBasePath());
		return new io.github.kotlinmania.spring.boot.webmvc.actuate.endpoint.web.ControllerEndpointHandlerMapping(
				endpointMapping, controllerEndpointsSupplier.getEndpoints(), corsProperties.toCorsConfiguration(),
				endpointAccessResolver);
	}

	@Bean
	@SuppressWarnings("removal")
	io.github.kotlinmania.spring.boot.actuate.endpoint.web.ServletEndpointRegistrar servletEndpointRegistrar(
			WebEndpointProperties properties,
			io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier servletEndpointsSupplier,
			DispatcherServletPath dispatcherServletPath, EndpointAccessResolver endpointAccessResolver) {
		return new io.github.kotlinmania.spring.boot.actuate.endpoint.web.ServletEndpointRegistrar(
				dispatcherServletPath.getRelativePath(properties.getBasePath()),
				servletEndpointsSupplier.getEndpoints(), endpointAccessResolver);
	}

	@Bean
	@ConditionalOnBean(EndpointJsonMapper.class)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static EndpointJsonMapperWebMvcConfigurer endpointJsonMapperWebMvcConfigurer(
			EndpointJsonMapper endpointJsonMapper) {
		return new EndpointJsonMapperWebMvcConfigurer(endpointJsonMapper);
	}

	@Bean
	@SuppressWarnings("removal")
	@ConditionalOnBean(io.github.kotlinmania.spring.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper.class)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static EndpointJackson2ObjectMapperWebMvcConfigurer endpointJackson2ObjectMapperWebMvcConfigurer(
			io.github.kotlinmania.spring.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper endpointJsonMapper) {
		return new EndpointJackson2ObjectMapperWebMvcConfigurer(endpointJsonMapper);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HealthEndpoint.class)
	static class HealthConfiguration {

		@Bean
		@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
		@ConditionalOnBean(HealthEndpoint.class)
		@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
		AdditionalHealthEndpointPathsWebMvcHandlerMapping managementHealthEndpointWebMvcHandlerMapping(
				WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups groups) {
			Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
			ExposableWebEndpoint healthEndpoint = webEndpoints.stream()
				.filter(this::isHealthEndpoint)
				.findFirst()
				.orElse(null);
			return new AdditionalHealthEndpointPathsWebMvcHandlerMapping(healthEndpoint,
					groups.getAllWithAdditionalPath(WebServerNamespace.MANAGEMENT));
		}

		private boolean isHealthEndpoint(ExposableWebEndpoint endpoint) {
			return endpoint.getEndpointId().equals(HealthEndpoint.ID);
		}

	}

	/**
	 * {@link WebMvcConfigurer} to apply {@link EndpointJsonMapper} for
	 * {@link OperationResponseBody} to {@link JacksonJsonHttpMessageConverter} instances.
	 */
	static class EndpointJsonMapperWebMvcConfigurer implements WebMvcConfigurer {

		private final EndpointJsonMapper mapper;

		EndpointJsonMapperWebMvcConfigurer(EndpointJsonMapper mapper) {
			this.mapper = mapper;
		}

		@Override
		public void configureMessageConverters(ServerBuilder builder) {
			builder.configureMessageConverters((converter) -> {
				if (converter instanceof JacksonJsonHttpMessageConverter jacksonConverter) {
					configure(jacksonConverter);
				}
			});
		}

		private void configure(JacksonJsonHttpMessageConverter converter) {
			converter.registerMappersForType(OperationResponseBody.class, (associations) -> {
				JsonMapper jsonMapper = this.mapper.get();
				MEDIA_TYPES.forEach((mimeType) -> associations.put(mimeType, jsonMapper));
			});
		}

	}

	/**
	 * {@link WebMvcConfigurer} to apply
	 * {@link io.github.kotlinmania.spring.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper}
	 * for {@link OperationResponseBody} to
	 * {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter}
	 * instances.
	 *
	 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3.
	 */
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	static class EndpointJackson2ObjectMapperWebMvcConfigurer implements WebMvcConfigurer {

		private final io.github.kotlinmania.spring.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper mapper;

		EndpointJackson2ObjectMapperWebMvcConfigurer(
				io.github.kotlinmania.spring.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper mapper) {
			this.mapper = mapper;
		}

		@Override
		public void configureMessageConverters(ServerBuilder builder) {
			builder.configureMessageConverters((converter) -> {
				if (converter instanceof org.springframework.http.converter.json.MappingJackson2HttpMessageConverter jacksonConverter) {
					configure(jacksonConverter);
				}
			});
		}

		private void configure(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter) {
			converter.registerObjectMappersForType(OperationResponseBody.class, (associations) -> {
				com.fasterxml.jackson.databind.ObjectMapper jsonMapper = this.mapper.get();
				MEDIA_TYPES.forEach((mimeType) -> associations.put(mimeType, jsonMapper));
			});
		}

	}

}
