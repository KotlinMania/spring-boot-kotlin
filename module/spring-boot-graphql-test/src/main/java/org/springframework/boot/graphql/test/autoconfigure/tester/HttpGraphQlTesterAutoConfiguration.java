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

package io.github.kotlinmania.spring.boot.graphql.test.autoconfigure.tester;

import org.jspecify.annotations.Nullable;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.graphql.autoconfigure.GraphQlProperties;
import io.github.kotlinmania.spring.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for {@link HttpGraphQlTester}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(afterName = "io.github.kotlinmania.spring.boot.webtestclient.autoconfigure.WebTestClientAutoConfiguration")
@ConditionalOnClass({ WebClient.class, WebTestClient.class, WebGraphQlTester.class })
@EnableConfigurationProperties(GraphQlProperties.class)
public final class HttpGraphQlTesterAutoConfiguration {

	@Bean
	@ConditionalOnBean(WebTestClient.class)
	@ConditionalOnMissingBean
	HttpGraphQlTester webTestClientGraphQlTester(ApplicationContext applicationContext, WebTestClient webTestClient,
			GraphQlProperties properties) {
		String graphQlPath = properties.getHttp().getPath();
		LocalTestWebServer localTestWebServer = LocalTestWebServer.get(applicationContext);
		return HttpGraphQlTester.create(createWebTestClient(webTestClient.mutate(), localTestWebServer, graphQlPath));
	}

	private WebTestClient createWebTestClient(WebTestClient.Builder builder,
			@Nullable LocalTestWebServer localTestWebServer, String graphQlPath) {
		return (localTestWebServer != null)
				? builder.uriBuilderFactory(localTestWebServer.withPath(graphQlPath).uriBuilderFactory()).build()
				: builder.baseUrl(graphQlPath).build();
	}

}
