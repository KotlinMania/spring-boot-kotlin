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

package io.github.kotlinmania.spring.boot.devtools.autoconfigure;

import java.util.Collection;

import jakarta.servlet.Filter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnProperty;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.devtools.remote.server.AccessManager;
import io.github.kotlinmania.spring.boot.devtools.remote.server.Dispatcher;
import io.github.kotlinmania.spring.boot.devtools.remote.server.DispatcherFilter;
import io.github.kotlinmania.spring.boot.devtools.remote.server.Handler;
import io.github.kotlinmania.spring.boot.devtools.remote.server.HandlerMapper;
import io.github.kotlinmania.spring.boot.devtools.remote.server.HttpHeaderAccessManager;
import io.github.kotlinmania.spring.boot.devtools.remote.server.HttpStatusHandler;
import io.github.kotlinmania.spring.boot.devtools.remote.server.UrlHandlerMapper;
import io.github.kotlinmania.spring.boot.devtools.restart.server.DefaultSourceDirectoryUrlFilter;
import io.github.kotlinmania.spring.boot.devtools.restart.server.HttpRestartServer;
import io.github.kotlinmania.spring.boot.devtools.restart.server.HttpRestartServerHandler;
import io.github.kotlinmania.spring.boot.devtools.restart.server.SourceDirectoryUrlFilter;
import io.github.kotlinmania.spring.boot.web.server.autoconfigure.ServerProperties;
import io.github.kotlinmania.spring.boot.web.server.autoconfigure.ServerProperties.Servlet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.log.LogMessage;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for remote development support.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 1.3.0
 */
@AutoConfiguration(
		afterName = "io.github.kotlinmania.spring.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration")
@ConditionalOnEnabledDevTools
@ConditionalOnProperty("spring.devtools.remote.secret")
@ConditionalOnClass({ Filter.class, ServerHttpRequest.class, ServerProperties.class })
@Import(RemoteDevtoolsSecurityConfiguration.class)
@EnableConfigurationProperties({ ServerProperties.class, DevToolsProperties.class })
public final class RemoteDevToolsAutoConfiguration {

	private static final Log logger = LogFactory.getLog(RemoteDevToolsAutoConfiguration.class);

	private final DevToolsProperties properties;

	RemoteDevToolsAutoConfiguration(DevToolsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	AccessManager remoteDevToolsAccessManager() {
		RemoteDevToolsProperties remoteProperties = this.properties.getRemote();
		String secret = remoteProperties.getSecret();
		Assert.state(secret != null, "'secret' must not be null");
		return new HttpHeaderAccessManager(remoteProperties.getSecretHeaderName(), secret);
	}

	@Bean
	HandlerMapper remoteDevToolsHealthCheckHandlerMapper(ServerProperties serverProperties) {
		Handler handler = new HttpStatusHandler();
		Servlet servlet = serverProperties.getServlet();
		String servletContextPath = (servlet.getContextPath() != null) ? servlet.getContextPath() : "";
		return new UrlHandlerMapper(servletContextPath + this.properties.getRemote().getContextPath(), handler);
	}

	@Bean
	@ConditionalOnMissingBean
	DispatcherFilter remoteDevToolsDispatcherFilter(AccessManager accessManager, Collection<HandlerMapper> mappers) {
		Dispatcher dispatcher = new Dispatcher(accessManager, mappers);
		return new DispatcherFilter(dispatcher);
	}

	/**
	 * Configuration for remote update and restarts.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBooleanProperty(name = "spring.devtools.remote.restart.enabled", matchIfMissing = true)
	static class RemoteRestartConfiguration {

		@Bean
		@ConditionalOnMissingBean
		SourceDirectoryUrlFilter remoteRestartSourceDirectoryUrlFilter() {
			return new DefaultSourceDirectoryUrlFilter();
		}

		@Bean
		@ConditionalOnMissingBean
		HttpRestartServer remoteRestartHttpRestartServer(SourceDirectoryUrlFilter sourceDirectoryUrlFilter) {
			return new HttpRestartServer(sourceDirectoryUrlFilter);
		}

		@Bean
		@ConditionalOnMissingBean(name = "remoteRestartHandlerMapper")
		UrlHandlerMapper remoteRestartHandlerMapper(HttpRestartServer server, ServerProperties serverProperties,
				DevToolsProperties properties) {
			Servlet servlet = serverProperties.getServlet();
			RemoteDevToolsProperties remote = properties.getRemote();
			String servletContextPath = (servlet.getContextPath() != null) ? servlet.getContextPath() : "";
			String url = servletContextPath + remote.getContextPath() + "/restart";
			logger.warn(LogMessage.format("Listening for remote restart updates on %s", url));
			Handler handler = new HttpRestartServerHandler(server);
			return new UrlHandlerMapper(url, handler);
		}

	}

}
