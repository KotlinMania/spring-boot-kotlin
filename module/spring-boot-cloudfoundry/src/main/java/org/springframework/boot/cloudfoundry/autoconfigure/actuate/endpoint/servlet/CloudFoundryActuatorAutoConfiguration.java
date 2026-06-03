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

package io.github.kotlinmania.spring.boot.cloudfoundry.autoconfigure.actuate.endpoint.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.endpoint.ExposableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.endpoint.invoke.ParameterValueMapper;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointMapping;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.EndpointMediaTypes;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.ExposableWebEndpoint;
import io.github.kotlinmania.spring.boot.actuate.info.GitInfoContributor;
import io.github.kotlinmania.spring.boot.actuate.info.InfoContributor;
import io.github.kotlinmania.spring.boot.actuate.info.InfoEndpoint;
import io.github.kotlinmania.spring.boot.actuate.info.InfoPropertiesInfoContributor;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.cloud.CloudPlatform;
import io.github.kotlinmania.spring.boot.cloudfoundry.autoconfigure.actuate.endpoint.CloudFoundryWebEndpointDiscoverer;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpoint;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpointWebExtension;
import io.github.kotlinmania.spring.boot.info.GitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to expose actuator endpoints for
 * Cloud Foundry to use.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration(after = InfoEndpointAutoConfiguration.class, afterName = {
		"io.github.kotlinmania.spring.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration",
		"io.github.kotlinmania.spring.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration" })
@ConditionalOnBooleanProperty(name = "management.cloudfoundry.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBean(DispatcherServlet.class)
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public final class CloudFoundryActuatorAutoConfiguration {

	private static final String BASE_PATH = "/cloudfoundryapplication";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	@ConditionalOnBean({ HealthEndpoint.class, HealthEndpointWebExtension.class })
	CloudFoundryHealthEndpointWebExtension cloudFoundryHealthEndpointWebExtension(
			HealthEndpointWebExtension healthEndpointWebExtension) {
		return new CloudFoundryHealthEndpointWebExtension(healthEndpointWebExtension);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	@ConditionalOnBean({ InfoEndpoint.class, GitProperties.class })
	CloudFoundryInfoEndpointWebExtension cloudFoundryInfoEndpointWebExtension(GitProperties properties,
			ObjectProvider<InfoContributor> infoContributors) {
		List<InfoContributor> contributors = infoContributors.orderedStream()
			.map((infoContributor) -> (infoContributor instanceof GitInfoContributor)
					? new GitInfoContributor(properties, InfoPropertiesInfoContributor.Mode.FULL) : infoContributor)
			.toList();
		return new CloudFoundryInfoEndpointWebExtension(new InfoEndpoint(contributors));
	}

	@Bean
	@SuppressWarnings("removal")
	CloudFoundryWebEndpointServletHandlerMapping cloudFoundryWebEndpointServletHandlerMapping(
			ParameterValueMapper parameterMapper, EndpointMediaTypes endpointMediaTypes,
			ObjectProvider<RestClient.Builder> restClientBuilder,
			io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier servletEndpointsSupplier,
			io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier controllerEndpointsSupplier,
			ApplicationContext applicationContext) {
		CloudFoundryWebEndpointDiscoverer discoverer = new CloudFoundryWebEndpointDiscoverer(applicationContext,
				parameterMapper, endpointMediaTypes, null, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList());
		SecurityInterceptor securityInterceptor = getSecurityInterceptor(
				restClientBuilder.getIfAvailable(RestClient::builder), applicationContext.getEnvironment());
		Collection<ExposableWebEndpoint> webEndpoints = discoverer.getEndpoints();
		List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
		allEndpoints.addAll(webEndpoints);
		allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
		allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
		return new CloudFoundryWebEndpointServletHandlerMapping(new EndpointMapping(BASE_PATH), webEndpoints,
				endpointMediaTypes, getCorsConfiguration(), securityInterceptor, allEndpoints);
	}

	private SecurityInterceptor getSecurityInterceptor(RestClient.Builder restClientBuilder, Environment environment) {
		SecurityService cloudfoundrySecurityService = getCloudFoundrySecurityService(restClientBuilder, environment);
		TokenValidator tokenValidator = (cloudfoundrySecurityService != null)
				? new TokenValidator(cloudfoundrySecurityService) : null;
		return new SecurityInterceptor(tokenValidator, cloudfoundrySecurityService,
				environment.getProperty("vcap.application.application_id"));
	}

	private @Nullable SecurityService getCloudFoundrySecurityService(RestClient.Builder restClientBuilder,
			Environment environment) {
		String cloudControllerUrl = environment.getProperty("vcap.application.cf_api");
		boolean skipSslValidation = environment.getProperty("management.cloudfoundry.skip-ssl-validation",
				Boolean.class, false);
		return (cloudControllerUrl != null)
				? new SecurityService(restClientBuilder, cloudControllerUrl, skipSslValidation) : null;
	}

	private static CorsConfiguration getCorsConfiguration() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.addAllowedOrigin(CorsConfiguration.ALL);
		corsConfiguration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
		corsConfiguration
			.setAllowedHeaders(Arrays.asList(HttpHeaders.AUTHORIZATION, "X-Cf-App-Instance", HttpHeaders.CONTENT_TYPE));
		return corsConfiguration;
	}

	/**
	 * {@link WebSecurityConfigurer} to tell Spring Security to permit cloudfoundry
	 * specific paths. The Cloud foundry endpoints are protected by their own security
	 * interceptor.
	 */
	@ConditionalOnClass({ WebSecurityCustomizer.class, WebSecurity.class })
	@Configuration(proxyBeanMethods = false)
	static class IgnoredCloudFoundryPathsWebSecurityConfiguration {

		private static final int FILTER_CHAIN_ORDER = -1;

		@Bean
		@Order(FILTER_CHAIN_ORDER)
		SecurityFilterChain cloudFoundrySecurityFilterChain(HttpSecurity http) throws Exception {
			RequestMatcher cloudFoundryRequest = getRequestMatcher();
			http.csrf((csrf) -> csrf.ignoringRequestMatchers(cloudFoundryRequest));
			CorsConfiguration corsConfiguration = getCorsConfiguration();
			http.cors((cors) -> cors.configurationSource((request) -> corsConfiguration));
			http.securityMatchers((matches) -> matches.requestMatchers(cloudFoundryRequest))
				.authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll());
			return http.build();
		}

		private RequestMatcher getRequestMatcher() {
			return pathMatcher(BASE_PATH + "/**");
		}

		private PathPatternRequestMatcher pathMatcher(String path) {
			return PathPatternRequestMatcher.withDefaults().matcher(path);
		}

	}

}
