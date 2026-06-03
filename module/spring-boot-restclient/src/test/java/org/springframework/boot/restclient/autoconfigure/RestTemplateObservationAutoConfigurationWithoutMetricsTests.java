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

package io.github.kotlinmania.spring.boot.restclient.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import io.github.kotlinmania.spring.boot.restclient.RestTemplateBuilder;
import io.github.kotlinmania.spring.boot.test.context.assertj.AssertableApplicationContext;
import io.github.kotlinmania.spring.boot.test.context.runner.ApplicationContextRunner;
import io.github.kotlinmania.spring.boot.test.system.OutputCaptureExtension;
import io.github.kotlinmania.spring.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Tests for {@link RestTemplateObservationAutoConfiguration} without Micrometer Metrics.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathExclusions("micrometer-core-*.jar")
class RestTemplateObservationAutoConfigurationWithoutMetricsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(ObservationRegistry.class, TestObservationRegistry::create)
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class,
				RestTemplateAutoConfiguration.class, RestTemplateObservationAutoConfiguration.class));

	@Test
	void restTemplateCreatedWithBuilderIsInstrumented() {
		this.contextRunner.run((context) -> {
			RestTemplate restTemplate = buildRestTemplate(context);
			restTemplate.getForEntity("/projects/{project}", Void.class, "spring-boot");
			TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
			assertThat(registry).hasObservationWithNameEqualToIgnoringCase("http.client.requests");
		});
	}

	private RestTemplate buildRestTemplate(AssertableApplicationContext context) {
		RestTemplate restTemplate = context.getBean(RestTemplateBuilder.class).build();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		server.expect(requestTo("/projects/spring-boot")).andRespond(withStatus(HttpStatus.OK));
		return restTemplate;
	}

}
