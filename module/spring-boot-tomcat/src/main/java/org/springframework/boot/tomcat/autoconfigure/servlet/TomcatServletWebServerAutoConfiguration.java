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

package io.github.kotlinmania.spring.boot.tomcat.autoconfigure.servlet;

import jakarta.servlet.ServletRequest;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.SearchStrategy;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.tomcat.TomcatConnectorCustomizer;
import io.github.kotlinmania.spring.boot.tomcat.TomcatContextCustomizer;
import io.github.kotlinmania.spring.boot.tomcat.TomcatProtocolHandlerCustomizer;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.TomcatServerProperties;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.TomcatWebServerConfiguration;
import io.github.kotlinmania.spring.boot.tomcat.servlet.TomcatServletWebServerFactory;
import io.github.kotlinmania.spring.boot.web.server.autoconfigure.ServerProperties;
import io.github.kotlinmania.spring.boot.web.server.autoconfigure.servlet.ForwardedHeaderFilterCustomizer;
import io.github.kotlinmania.spring.boot.web.server.autoconfigure.servlet.ServletWebServerConfiguration;
import io.github.kotlinmania.spring.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a Tomcat-based servlet web
 * server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ServletRequest.class, Tomcat.class, UpgradeProtocol.class, TomcatServletWebServerFactory.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(TomcatServerProperties.class)
@Import({ ServletWebServerConfiguration.class, TomcatWebServerConfiguration.class })
public final class TomcatServletWebServerAutoConfiguration {

	private final TomcatServerProperties tomcatProperties;

	TomcatServletWebServerAutoConfiguration(TomcatServerProperties tomcatProperties) {
		this.tomcatProperties = tomcatProperties;
	}

	@Bean
	@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
	TomcatServletWebServerFactory tomcatServletWebServerFactory(
			ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
			ObjectProvider<TomcatContextCustomizer> contextCustomizers,
			ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
		factory.getConnectorCustomizers().addAll(connectorCustomizers.orderedStream().toList());
		factory.getContextCustomizers().addAll(contextCustomizers.orderedStream().toList());
		factory.getProtocolHandlerCustomizers().addAll(protocolHandlerCustomizers.orderedStream().toList());
		return factory;
	}

	@Bean
	TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
			TomcatServerProperties tomcatProperties) {
		return new TomcatServletWebServerFactoryCustomizer(tomcatProperties);
	}

	@Bean
	@ConditionalOnProperty(name = "server.forward-headers-strategy", havingValue = "framework")
	ForwardedHeaderFilterCustomizer tomcatForwardedHeaderFilterCustomizer(ServerProperties serverProperties) {
		return (filter) -> filter.setRelativeRedirects(this.tomcatProperties.isUseRelativeRedirects());
	}

}
