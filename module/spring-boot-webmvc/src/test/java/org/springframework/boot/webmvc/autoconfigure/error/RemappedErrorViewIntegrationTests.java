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

package io.github.kotlinmania.spring.boot.webmvc.autoconfigure.error;

import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import io.github.kotlinmania.spring.boot.builder.SpringApplicationBuilder;
import io.github.kotlinmania.spring.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import io.github.kotlinmania.spring.boot.resttestclient.TestRestTemplate;
import io.github.kotlinmania.spring.boot.test.context.SpringBootTest;
import io.github.kotlinmania.spring.boot.test.context.SpringBootTest.WebEnvironment;
import io.github.kotlinmania.spring.boot.test.web.server.LocalServerPort;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import io.github.kotlinmania.spring.boot.web.error.ErrorPage;
import io.github.kotlinmania.spring.boot.web.error.ErrorPageRegistrar;
import io.github.kotlinmania.spring.boot.web.error.ErrorPageRegistry;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for remapped error pages.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.mvc.servlet.path:/spring/")
@DirtiesContext
class RemappedErrorViewIntegrationTests {

	@LocalServerPort
	private int port;

	private final TestRestTemplate template = new TestRestTemplate();

	@Test
	void directAccessToErrorPage() {
		String content = this.template.getForObject("http://localhost:" + this.port + "/spring/error", String.class);
		assertThat(content).contains("error");
		assertThat(content).contains("999");
	}

	@Test
	void forwardToErrorPage() {
		String content = this.template.getForObject("http://localhost:" + this.port + "/spring/", String.class);
		assertThat(content).contains("error");
		assertThat(content).contains("500");
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, TomcatServletWebServerAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
	@Controller
	static class TestConfiguration implements ErrorPageRegistrar {

		@RequestMapping("/")
		String home() {
			throw new RuntimeException("Planned!");
		}

		@Override
		public void registerErrorPages(ErrorPageRegistry errorPageRegistry) {
			errorPageRegistry.addErrorPages(new ErrorPage("/spring/error"));
		}

		// For manual testing
		static void main(String[] args) {
			new SpringApplicationBuilder(TestConfiguration.class).properties("spring.mvc.servlet.path:spring/*")
				.run(args);
		}

	}

}
