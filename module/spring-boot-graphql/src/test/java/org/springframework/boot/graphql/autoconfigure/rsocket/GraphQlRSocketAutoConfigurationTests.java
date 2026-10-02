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

package io.github.kotlinmania.spring.boot.graphql.autoconfigure.rsocket;

import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;

import graphql.schema.idl.TypeRuntimeWiring;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import io.github.kotlinmania.spring.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import io.github.kotlinmania.spring.boot.graphql.autoconfigure.GraphQlTestDataFetchers;
import io.github.kotlinmania.spring.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import io.github.kotlinmania.spring.boot.logging.LogLevel;
import io.github.kotlinmania.spring.boot.reactor.netty.NettyReactiveWebServerFactory;
import io.github.kotlinmania.spring.boot.reactor.netty.NettyRouteProvider;
import io.github.kotlinmania.spring.boot.rsocket.autoconfigure.RSocketMessagingAutoConfiguration;
import io.github.kotlinmania.spring.boot.rsocket.autoconfigure.RSocketServerAutoConfiguration;
import io.github.kotlinmania.spring.boot.rsocket.autoconfigure.RSocketStrategiesAutoConfiguration;
import io.github.kotlinmania.spring.boot.rsocket.context.RSocketPortInfoApplicationContextInitializer;
import io.github.kotlinmania.spring.boot.test.context.FilteredClassLoader;
import io.github.kotlinmania.spring.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import io.github.kotlinmania.spring.boot.testsupport.classpath.resources.WithResource;
import io.github.kotlinmania.spring.boot.web.server.context.ServerPortInfoApplicationContextInitializer;
import io.github.kotlinmania.spring.boot.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import io.github.kotlinmania.spring.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
import io.github.kotlinmania.spring.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import io.github.kotlinmania.spring.boot.webflux.autoconfigure.error.ErrorWebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.RSocketGraphQlClient;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.GraphQlRSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphQlRSocketAutoConfiguration}
 *
 * @author Brian Clozel
 */
@WithResource(name = "graphql/types/book.graphqls", content = """
		type Book {
		    id: ID
		    name: String
		    pageCount: Int
		    author: String
		}
		""")
@WithResource(name = "graphql/schema.graphqls", content = """
		type Query {
		    greeting(name: String! = "Spring"): String!
		    bookById(id: ID): Book
		    books: BookConnection
		}

		type Subscription {
		    booksOnSale(minPages: Int) : Book!
		}
		""")
class GraphQlRSocketAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(JacksonAutoConfiguration.class, RSocketStrategiesAutoConfiguration.class,
						RSocketMessagingAutoConfiguration.class, RSocketServerAutoConfiguration.class,
						GraphQlAutoConfiguration.class, GraphQlRSocketAutoConfiguration.class))
		.withUserConfiguration(DataFetchersConfiguration.class)
		.withPropertyValues("spring.main.web-application-type=reactive", "spring.graphql.rsocket.mapping=graphql");

	@Test
	void shouldContributeDefaultBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(GraphQlRSocketHandler.class)
			.hasSingleBean(GraphQlRSocketController.class));
	}

	@Test
	void simpleQueryShouldWorkWithTcpServer() {
		testWithRSocketTcp(this::assertThatSimpleQueryWorks);
	}

	@Test
	void simpleQueryShouldWorkWithWebSocketServer() {
		testWithRSocketWebSocket(this::assertThatSimpleQueryWorks);
	}

	@Test
	void usesJacksonByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("jacksonJsonEncoderSupplier"));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void usesJackson2WhenItIsPreferred() {
		this.contextRunner.withPropertyValues("spring.graphql.rsocket.preferred-json-mapper=jackson2")
			.withConfiguration(AutoConfigurations
				.of(io.github.kotlinmania.spring.boot.jackson2.autoconfigure.Jackson2AutoConfiguration.class))
			.run((context) -> assertThat(context).hasBean("jackson2JsonEncoderSupplier"));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void usesJackson2WhenJacksonIsAbsent() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(Thread.currentThread().getContextClassLoader(), JsonMapper.class))
			.withConfiguration(AutoConfigurations
				.of(io.github.kotlinmania.spring.boot.jackson2.autoconfigure.Jackson2AutoConfiguration.class))
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run((context) -> assertThat(context).hasBean("jackson2JsonEncoderSupplier"));
	}

	private void assertThatSimpleQueryWorks(RSocketGraphQlClient client) {
		String document = "{ bookById(id: \"book-1\"){ id name pageCount author } }";
		String bookName = client.document(document)
			.retrieve("bookById.name")
			.toEntity(String.class)
			.block(Duration.ofSeconds(5));
		assertThat(bookName).isEqualTo("GraphQL for beginners");
	}

	private void testWithRSocketTcp(Consumer<RSocketGraphQlClient> consumer) {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(JacksonAutoConfiguration.class, RSocketStrategiesAutoConfiguration.class,
							RSocketMessagingAutoConfiguration.class, RSocketServerAutoConfiguration.class,
							GraphQlAutoConfiguration.class, GraphQlRSocketAutoConfiguration.class))
			.withUserConfiguration(DataFetchersConfiguration.class)
			.withPropertyValues("spring.main.web-application-type=reactive", "spring.graphql.rsocket.mapping=graphql");
		contextRunner.withInitializer(new RSocketPortInfoApplicationContextInitializer())
			.withPropertyValues("spring.rsocket.server.port=0")
			.run((context) -> {
				String serverPort = context.getEnvironment().getProperty("local.rsocket.server.port");
				RSocketGraphQlClient client = RSocketGraphQlClient.builder()
					.tcp("localhost", Integer.parseInt(serverPort))
					.route("graphql")
					.build();
				consumer.accept(client);
			});
	}

	private void testWithRSocketWebSocket(Consumer<RSocketGraphQlClient> consumer) {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner(
				AnnotationConfigReactiveWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class,
					ErrorWebFluxAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
					JacksonAutoConfiguration.class, RSocketStrategiesAutoConfiguration.class,
					RSocketMessagingAutoConfiguration.class, RSocketServerAutoConfiguration.class,
					GraphQlAutoConfiguration.class, GraphQlRSocketAutoConfiguration.class))
			.withInitializer(new ServerPortInfoApplicationContextInitializer())
			.withUserConfiguration(DataFetchersConfiguration.class, NettyServerConfiguration.class)
			.withPropertyValues("spring.main.web-application-type=reactive", "server.port=0",
					"spring.graphql.rsocket.mapping=graphql", "spring.rsocket.server.transport=websocket",
					"spring.rsocket.server.mapping-path=/rsocket");
		contextRunner.run((context) -> {
			String serverPort = context.getEnvironment().getProperty("local.server.port");
			RSocketGraphQlClient client = RSocketGraphQlClient.builder()
				.webSocket(URI.create("ws://localhost:" + serverPort + "/rsocket"))
				.route("graphql")
				.build();
			consumer.accept(client);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class NettyServerConfiguration {

		@Bean
		NettyReactiveWebServerFactory serverFactory(NettyRouteProvider routeProvider) {
			NettyReactiveWebServerFactory serverFactory = new NettyReactiveWebServerFactory(0);
			serverFactory.addRouteProviders(routeProvider);
			return serverFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DataFetchersConfiguration {

		@Bean
		RuntimeWiringConfigurer bookDataFetcher() {
			return (builder) -> builder.type(TypeRuntimeWiring.newTypeWiring("Query")
				.dataFetcher("bookById", GraphQlTestDataFetchers.getBookByIdDataFetcher()));
		}

	}

}
