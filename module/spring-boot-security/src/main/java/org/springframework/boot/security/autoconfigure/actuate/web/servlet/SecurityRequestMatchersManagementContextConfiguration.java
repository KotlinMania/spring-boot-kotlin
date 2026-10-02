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

package io.github.kotlinmania.spring.boot.security.autoconfigure.actuate.web.servlet;

import org.glassfish.jersey.server.ResourceConfig;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.jersey.autoconfigure.JerseyApplicationPath;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.DispatcherServletPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link ManagementContextConfiguration} that configures the appropriate
 * {@link RequestMatcherProvider}.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnClass({ RequestMatcher.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
public class SecurityRequestMatchersManagementContextConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(DispatcherServlet.class)
	@ConditionalOnBean(DispatcherServletPath.class)
	public static class MvcRequestMatcherConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public RequestMatcherProvider requestMatcherProvider(DispatcherServletPath servletPath) {
			return new PathPatternRequestMatcherProvider(servletPath::getRelativePath);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ResourceConfig.class)
	@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
	@ConditionalOnBean(JerseyApplicationPath.class)
	public static class JerseyRequestMatcherConfiguration {

		@Bean
		public RequestMatcherProvider requestMatcherProvider(JerseyApplicationPath applicationPath) {
			return new PathPatternRequestMatcherProvider(applicationPath::getRelativePath);
		}

	}

}
