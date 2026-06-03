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

package io.github.kotlinmania.spring.boot.graphql.autoconfigure.security;

import graphql.schema.idl.TypeRuntimeWiring;
import org.assertj.core.api.ThrowingConsumer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.graphql.autoconfigure.Book;
import io.github.kotlinmania.spring.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import io.github.kotlinmania.spring.boot.graphql.autoconfigure.GraphQlTestDataFetchers;
import io.github.kotlinmania.spring.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration;
import io.github.kotlinmania.spring.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import io.github.kotlinmania.spring.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import io.github.kotlinmania.spring.boot.security.autoconfigure.SecurityAutoConfiguration;
import io.github.kotlinmania.spring.boot.test.context.runner.WebApplicationContextRunner;
import io.github.kotlinmania.spring.boot.testsupport.classpath.resources.WithResource;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.execution.SecurityDataFetcherExceptionResolver;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Tests for {@link GraphQlWebMvcSecurityAutoConfiguration}.
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
class GraphQlWebMvcSecurityAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class,
				WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
				JacksonAutoConfiguration.class, GraphQlAutoConfiguration.class, GraphQlWebMvcAutoConfiguration.class,
				GraphQlWebMvcSecurityAutoConfiguration.class, SecurityAutoConfiguration.class))
		.withUserConfiguration(DataFetchersConfiguration.class, SecurityConfig.class)
		.withPropertyValues("spring.main.web-application-type=servlet");

	@Test
	void contributesSecurityComponents() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(SecurityDataFetcherExceptionResolver.class));
	}

	@Test
	void anonymousUserShouldBeUnauthorized() {
		withMockMvc((mvc) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author }}";
			assertThat(mvc.post().uri("/graphql").content("{\"query\": \"" + query + "\"}")).satisfies((result) -> {
				assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
				assertThat(result).bodyJson()
					.doesNotHavePath("data.bookById.name")
					.extractingPath("errors[0].extensions.classification")
					.asString()
					.isEqualTo(ErrorType.UNAUTHORIZED.toString());
			});
		});
	}

	@Test
	void authenticatedUserShouldGetData() {
		withMockMvc((mvc) -> {
			String query = "{  bookById(id: \\\"book-1\\\"){ id name pageCount author }}";
			assertThat(mvc.post().uri("/graphql").content("{\"query\": \"" + query + "\"}").with(user("rob")))
				.satisfies((result) -> {
					assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
					assertThat(result).bodyJson()
						.doesNotHavePath("errors")
						.extractingPath("data.bookById.name")
						.asString()
						.isEqualTo("GraphQL for beginners");
				});
		});
	}

	private void withMockMvc(ThrowingConsumer<MockMvcTester> mvc) {
		this.contextRunner.run((context) -> {
			MediaType mediaType = MediaType.APPLICATION_JSON;
			MockMvcTester mockMVc = MockMvcTester.from(context,
					(builder) -> builder.defaultRequest(post("/graphql").contentType(mediaType).accept(mediaType))
						.apply(springSecurity())
						.build());
			mvc.accept(mockMVc);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class DataFetchersConfiguration {

		@Bean
		RuntimeWiringConfigurer bookDataFetcher(BookService bookService) {
			return (builder) -> builder.type(TypeRuntimeWiring.newTypeWiring("Query").dataFetcher("bookById", (env) -> {
				String id = env.getArgument("id");
				assertThat(id).isNotNull();
				return bookService.getBookdById(id);
			}));
		}

		@Bean
		BookService bookService() {
			return new BookService();
		}

	}

	static class BookService {

		@PreAuthorize("hasRole('USER')")
		@Nullable Book getBookdById(String id) {
			return GraphQlTestDataFetchers.getBookById(id);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	@EnableMethodSecurity(prePostEnabled = true)
	@SuppressWarnings("deprecation")
	static class SecurityConfig {

		@Bean
		DefaultSecurityFilterChain springWebFilterChain(HttpSecurity http) {
			return http.csrf(CsrfConfigurer::disable)
				// Demonstrate that method security works
				// Best practice to use both for defense in depth
				.authorizeHttpRequests((requests) -> requests.anyRequest().permitAll())
				.httpBasic(withDefaults())
				.build();
		}

		@Bean
		InMemoryUserDetailsManager userDetailsService() {
			User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();
			UserDetails rob = userBuilder.username("rob").password("rob").roles("USER").build();
			UserDetails admin = userBuilder.username("admin").password("admin").roles("USER", "ADMIN").build();
			return new InMemoryUserDetailsManager(rob, admin);
		}

	}

}
