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

package io.github.kotlinmania.spring.boot.jetty.autoconfigure.actuate.web.server;

import org.eclipse.jetty.server.Server;

import io.github.kotlinmania.spring.boot.WebApplicationType;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ManagementContextFactory;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ManagementPortType;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.jetty.autoconfigure.reactive.JettyReactiveWebServerAutoConfiguration;
import io.github.kotlinmania.spring.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for a Jetty-based reactive management context.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ Server.class, ManagementContextFactory.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
public final class JettyReactiveManagementContextAutoConfiguration {

	@Bean
	static ManagementContextFactory reactiveWebChildContextFactory() {
		return new ManagementContextFactory(WebApplicationType.REACTIVE, ReactiveWebServerFactory.class,
				JettyReactiveWebServerAutoConfiguration.class);
	}

}
