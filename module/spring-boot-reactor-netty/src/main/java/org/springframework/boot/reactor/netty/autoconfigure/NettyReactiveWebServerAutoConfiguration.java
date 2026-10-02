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

package io.github.kotlinmania.spring.boot.reactor.netty.autoconfigure;

import reactor.netty.http.server.HttpServer;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.reactor.netty.NettyReactiveWebServerFactory;
import io.github.kotlinmania.spring.boot.reactor.netty.NettyRouteProvider;
import io.github.kotlinmania.spring.boot.reactor.netty.NettyServerCustomizer;
import io.github.kotlinmania.spring.boot.reactor.netty.autoconfigure.ReactorNettyConfigurations.ReactorResourceFactoryConfiguration;
import io.github.kotlinmania.spring.boot.web.server.autoconfigure.ServerProperties;
import io.github.kotlinmania.spring.boot.web.server.autoconfigure.reactive.ReactiveWebServerConfiguration;
import io.github.kotlinmania.spring.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.client.ReactorResourceFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a Netty-based reactive web
 * server.
 *
 * @author Andy Wilkinson
 * @author Daeho Kwon
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ReactiveHttpInputMessage.class, HttpServer.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(NettyServerProperties.class)
@Import({ ReactiveWebServerConfiguration.class, ReactorResourceFactoryConfiguration.class })
public final class NettyReactiveWebServerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	NettyReactiveWebServerFactory nettyReactiveWebServerFactory(ReactorResourceFactory resourceFactory,
			ObjectProvider<NettyRouteProvider> routes, ObjectProvider<NettyServerCustomizer> serverCustomizers) {
		NettyReactiveWebServerFactory serverFactory = new NettyReactiveWebServerFactory();
		serverFactory.setResourceFactory(resourceFactory);
		routes.orderedStream().forEach(serverFactory::addRouteProviders);
		serverFactory.getServerCustomizers().addAll(serverCustomizers.orderedStream().toList());
		return serverFactory;
	}

	@Bean
	NettyReactiveWebServerFactoryCustomizer nettyWebServerFactoryCustomizer(Environment environment,
			ServerProperties serverProperties, NettyServerProperties nettyProperties) {
		return new NettyReactiveWebServerFactoryCustomizer(environment, serverProperties, nettyProperties);
	}

}
