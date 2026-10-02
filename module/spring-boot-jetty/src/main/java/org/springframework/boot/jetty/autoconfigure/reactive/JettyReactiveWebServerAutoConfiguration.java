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

package io.github.kotlinmania.spring.boot.jetty.autoconfigure.reactive;

import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.jetty.JettyServerCustomizer;
import io.github.kotlinmania.spring.boot.jetty.autoconfigure.JettyServerProperties;
import io.github.kotlinmania.spring.boot.jetty.autoconfigure.JettyWebServerConfiguration;
import io.github.kotlinmania.spring.boot.jetty.reactive.JettyReactiveWebServerFactory;
import io.github.kotlinmania.spring.boot.web.server.autoconfigure.reactive.ReactiveWebServerConfiguration;
import io.github.kotlinmania.spring.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ReactiveHttpInputMessage;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a Jetty-based reactive web
 * server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ Server.class, ServletHolder.class, ReactiveHttpInputMessage.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(JettyServerProperties.class)
@Import({ JettyWebServerConfiguration.class, ReactiveWebServerConfiguration.class })
public final class JettyReactiveWebServerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	JettyReactiveWebServerFactory jettyReactiveWebServerFactory(
			ObjectProvider<JettyServerCustomizer> serverCustomizers) {
		JettyReactiveWebServerFactory serverFactory = new JettyReactiveWebServerFactory();
		serverFactory.getServerCustomizers().addAll(serverCustomizers.orderedStream().toList());
		return serverFactory;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JakartaWebSocketServletContainerInitializer.class)
	static class JettyWebSocketConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "websocketReactiveWebServerCustomizer")
		WebSocketJettyReactiveWebServerFactoryCustomizer websocketServletWebServerCustomizer() {
			return new WebSocketJettyReactiveWebServerFactoryCustomizer();
		}

	}

}
