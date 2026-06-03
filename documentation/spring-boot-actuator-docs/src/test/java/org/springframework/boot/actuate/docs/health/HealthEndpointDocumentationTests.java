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

package io.github.kotlinmania.spring.boot.actuate.docs.health;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.actuate.docs.MockMvcEndpointDocumentationTests;
import io.github.kotlinmania.spring.boot.actuate.endpoint.SecurityContext;
import io.github.kotlinmania.spring.boot.autoconfigure.ImportAutoConfiguration;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.AdditionalHealthEndpointPath;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpoint;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpointGroup;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HealthEndpointGroups;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import io.github.kotlinmania.spring.boot.health.actuate.endpoint.StatusAggregator;
import io.github.kotlinmania.spring.boot.health.application.DiskSpaceHealthIndicator;
import io.github.kotlinmania.spring.boot.health.autoconfigure.registry.HealthContributorNameGenerator;
import io.github.kotlinmania.spring.boot.health.contributor.CompositeHealthContributor;
import io.github.kotlinmania.spring.boot.health.contributor.Health;
import io.github.kotlinmania.spring.boot.health.contributor.HealthContributor;
import io.github.kotlinmania.spring.boot.health.contributor.HealthIndicator;
import io.github.kotlinmania.spring.boot.health.registry.DefaultHealthContributorRegistry;
import io.github.kotlinmania.spring.boot.health.registry.HealthContributorRegistry;
import io.github.kotlinmania.spring.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import io.github.kotlinmania.spring.boot.jdbc.health.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;

/**
 * Tests for generating documentation describing the {@link HealthEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class HealthEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final List<FieldDescriptor> componentFields = List.of(
			fieldWithPath("status").description("Status of a specific part of the application"),
			subsectionWithPath("details").description("Details of the health of a specific part of the application."));

	@Test
	void health() {
		FieldDescriptor status = fieldWithPath("status").description("Overall status of the application.");
		FieldDescriptor components = fieldWithPath("components").description("The components that make up the health.");
		FieldDescriptor componentStatus = fieldWithPath("components.*.status")
			.description("Status of a specific part of the application.");
		FieldDescriptor nestedComponents = subsectionWithPath("components.*.components")
			.description("The nested components that make up the health.")
			.optional();
		FieldDescriptor componentDetails = subsectionWithPath("components.*.details")
			.description("Details of the health of a specific part of the application. "
					+ "Presence is controlled by `management.endpoint.health.show-details`.")
			.optional();
		assertThat(this.mvc.get().uri("/actuator/health").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.apply(document("health",
					responseFields(status, components, componentStatus, nestedComponents, componentDetails)));
	}

	@Test
	void healthComponent() {
		assertThat(this.mvc.get().uri("/actuator/health/db").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.apply(document("health/component", responseFields(componentFields)));
	}

	@Test
	void healthComponentInstance() {
		assertThat(this.mvc.get().uri("/actuator/health/broker/us1").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.apply(document("health/instance", responseFields(componentFields)));
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		HealthEndpoint healthEndpoint(Map<String, HealthContributor> contributors) {
			HealthContributorRegistry registry = new DefaultHealthContributorRegistry(null,
					HealthContributorNameGenerator.withoutStandardSuffixes().registrar(contributors));
			HealthEndpointGroup primary = new TestHealthEndpointGroup();
			HealthEndpointGroups groups = HealthEndpointGroups.of(primary, Collections.emptyMap());
			return new HealthEndpoint(registry, null, groups, null);
		}

		@Bean
		DiskSpaceHealthIndicator diskSpaceHealthIndicator() {
			return new DiskSpaceHealthIndicator(new File("."), DataSize.ofMegabytes(10));
		}

		@Bean
		DataSourceHealthIndicator dbHealthIndicator(DataSource dataSource) {
			return new DataSourceHealthIndicator(dataSource);
		}

		@Bean
		CompositeHealthContributor brokerHealthContributor() {
			Map<String, HealthIndicator> indicators = new LinkedHashMap<>();
			indicators.put("us1", () -> Health.up().withDetail("version", "1.0.2").build());
			indicators.put("us2", () -> Health.up().withDetail("version", "1.0.4").build());
			return CompositeHealthContributor.fromMap(indicators);
		}

	}

	private static final class TestHealthEndpointGroup implements HealthEndpointGroup {

		private final StatusAggregator statusAggregator = StatusAggregator.getDefault();

		private final HttpCodeStatusMapper httpCodeStatusMapper = HttpCodeStatusMapper.getDefault();

		@Override
		public boolean isMember(String name) {
			return true;
		}

		@Override
		public boolean showComponents(SecurityContext securityContext) {
			return true;
		}

		@Override
		public boolean showDetails(SecurityContext securityContext) {
			return true;
		}

		@Override
		public StatusAggregator getStatusAggregator() {
			return this.statusAggregator;
		}

		@Override
		public HttpCodeStatusMapper getHttpCodeStatusMapper() {
			return this.httpCodeStatusMapper;
		}

		@Override
		public AdditionalHealthEndpointPath getAdditionalPath() {
			return null;
		}

	}

}
