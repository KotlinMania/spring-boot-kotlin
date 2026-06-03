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

package io.github.kotlinmania.spring.boot.tomcat.autoconfigure.reactive;

import org.apache.catalina.startup.Tomcat;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.tomcat.TomcatConnectorCustomizer;
import io.github.kotlinmania.spring.boot.tomcat.TomcatContextCustomizer;
import io.github.kotlinmania.spring.boot.tomcat.TomcatProtocolHandlerCustomizer;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.TomcatServerProperties;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.TomcatWebServerConfiguration;
import io.github.kotlinmania.spring.boot.tomcat.reactive.TomcatReactiveWebServerFactory;
import io.github.kotlinmania.spring.boot.web.server.autoconfigure.reactive.ReactiveWebServerConfiguration;
import io.github.kotlinmania.spring.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ReactiveHttpInputMessage;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a Tomcat-based reactive web
 * server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ReactiveHttpInputMessage.class, Tomcat.class, TomcatReactiveWebServerFactory.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(TomcatServerProperties.class)
@Import({ TomcatWebServerConfiguration.class, ReactiveWebServerConfiguration.class })
public final class TomcatReactiveWebServerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	TomcatReactiveWebServerFactory tomcatReactiveWebServerFactory(
			ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
			ObjectProvider<TomcatContextCustomizer> contextCustomizers,
			ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		factory.getConnectorCustomizers().addAll(connectorCustomizers.orderedStream().toList());
		factory.getContextCustomizers().addAll(contextCustomizers.orderedStream().toList());
		factory.getProtocolHandlerCustomizers().addAll(protocolHandlerCustomizers.orderedStream().toList());
		return factory;
	}

}
