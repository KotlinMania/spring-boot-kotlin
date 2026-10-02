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

package io.github.kotlinmania.spring.boot.servlet.autoconfigure.actuate.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.kotlinmania.spring.boot.SpringApplication;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.autoconfigure.ImportAutoConfiguration;
import io.github.kotlinmania.spring.boot.env.PropertySourceInfo;
import io.github.kotlinmania.spring.boot.test.context.runner.WebApplicationContextRunner;
import io.github.kotlinmania.spring.boot.test.system.CapturedOutput;
import io.github.kotlinmania.spring.boot.test.system.OutputCaptureExtension;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.actuate.web.server.TomcatServletManagementContextAutoConfiguration;
import io.github.kotlinmania.spring.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import io.github.kotlinmania.spring.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ServletManagementContextAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class ServletManagementContextAutoConfigurationIntegrationTests {

	@Test
	void childManagementContextShouldStartForEmbeddedServer(CapturedOutput output) {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
					TomcatServletWebServerAutoConfiguration.class,
					TomcatServletManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					EndpointAutoConfiguration.class));
		contextRunner.withPropertyValues("server.port=0", "management.server.port=0")
			.run((context) -> assertThat(output).satisfies(numberOfOccurrences("Tomcat started on port", 2)));
	}

	@Test
	void childManagementContextShouldNotStartWithoutEmbeddedServer(CapturedOutput output) {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
					TomcatServletWebServerAutoConfiguration.class,
					TomcatServletManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					EndpointAutoConfiguration.class));
		contextRunner.withPropertyValues("server.port=0", "management.server.port=0").run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(output).doesNotContain("Tomcat started");
		});
	}

	@Test
	void childManagementContextShouldRestartWhenParentIsStoppedThenStarted(CapturedOutput output) {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
					TomcatServletWebServerAutoConfiguration.class,
					TomcatServletManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					EndpointAutoConfiguration.class));
		contextRunner.withPropertyValues("server.port=0", "management.server.port=0").run((context) -> {
			assertThat(output).satisfies(numberOfOccurrences("Tomcat started on port", 2));
			context.getSourceApplicationContext().stop();
			context.getSourceApplicationContext().start();
			assertThat(output).satisfies(numberOfOccurrences("Tomcat started on port", 4));
		});
	}

	@Test
	void givenSamePortManagementServerWhenManagementServerAddressIsConfiguredThenContextRefreshFails() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
					TomcatServletWebServerAutoConfiguration.class, ServletManagementContextAutoConfiguration.class,
					WebEndpointAutoConfiguration.class, EndpointAutoConfiguration.class));
		contextRunner.withPropertyValues("server.port=0", "management.server.address=127.0.0.1")
			.run((context) -> assertThat(context).getFailure()
				.hasMessageStartingWith("Management-specific server address cannot be configured"));
	}

	@Test // gh-45858
	void childEnvironmentShouldInheritPrefix() throws Exception {
		SpringApplication application = new SpringApplication(ChildEnvironmentConfiguration.class);
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("server.port", "0");
		properties.put("management.server.port", "0");
		application.setDefaultProperties(properties);
		application.setEnvironmentPrefix("my");
		try (ConfigurableApplicationContext parentContext = application.run()) {
			Class<?> initializerClass = ClassUtils.forName(
					"io.github.kotlinmania.spring.boot.actuate.autoconfigure.web.server.ChildManagementContextInitializer",
					null);
			Object initializer = parentContext.getBean(initializerClass);
			ConfigurableApplicationContext managementContext = (ConfigurableApplicationContext) ReflectionTestUtils
				.getField(initializer, "managementContext");
			assertThat(managementContext).isNotNull();
			ConfigurableEnvironment managementEnvironment = managementContext.getEnvironment();
			assertThat(managementEnvironment).isNotNull();
			PropertySource<?> systemEnvironmentPropertySource = managementEnvironment.getPropertySources()
				.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
			assertThat(systemEnvironmentPropertySource).isNotNull();
			assertThat(((PropertySourceInfo) systemEnvironmentPropertySource).getPrefix()).isEqualTo("my");
		}
	}

	private <T extends CharSequence> Consumer<T> numberOfOccurrences(String substring, int expectedCount) {
		return (charSequence) -> {
			int count = StringUtils.countOccurrencesOf(charSequence.toString(), substring);
			assertThat(count).isEqualTo(expectedCount);
		};
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ ManagementContextAutoConfiguration.class, TomcatServletWebServerAutoConfiguration.class,
			TomcatServletManagementContextAutoConfiguration.class, ServletManagementContextAutoConfiguration.class,
			WebEndpointAutoConfiguration.class, EndpointAutoConfiguration.class })
	static class ChildEnvironmentConfiguration {

	}

}
