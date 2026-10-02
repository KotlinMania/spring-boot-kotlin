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

package io.github.kotlinmania.spring.boot.testcontainers.lifecycle;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.github.kotlinmania.spring.boot.context.properties.ConfigurationProperties;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.test.context.TestConfiguration;
import io.github.kotlinmania.spring.boot.testcontainers.context.ImportTestcontainers;
import io.github.kotlinmania.spring.boot.testcontainers.lifecycle.TestcontainersImportWithPropertiesInjectedIntoLoadTimeWeaverAwareBeanIntegrationTests.Containers;
import io.github.kotlinmania.spring.boot.testsupport.container.DisabledIfDockerUnavailable;
import io.github.kotlinmania.spring.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests for {@link ImportTestcontainers} when properties are being injected into a
 * {@link LoadTimeWeaverAware} bean.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@DisabledIfDockerUnavailable
@ImportTestcontainers(Containers.class)
class TestcontainersImportWithPropertiesInjectedIntoLoadTimeWeaverAwareBeanIntegrationTests {

	// gh-38913

	@Test
	void starts() {
	}

	@TestConfiguration
	@EnableConfigurationProperties(MockDataSourceProperties.class)
	static class Config {

		@Bean
		MockEntityManager mockEntityManager(MockDataSourceProperties properties) {
			return new MockEntityManager();
		}

	}

	static class MockEntityManager implements LoadTimeWeaverAware {

		@Override
		public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		}

	}

	@ConfigurationProperties("spring.datasource")
	public static class MockDataSourceProperties {

		private @Nullable String url;

		public @Nullable String getUrl() {
			return this.url;
		}

		public void setUrl(@Nullable String url) {
			this.url = url;
		}

	}

	static class Containers {

		@Container
		static PostgreSQLContainer container = TestImage.container(PostgreSQLContainer.class);

		@DynamicPropertySource
		static void setConnectionProperties(DynamicPropertyRegistry registry) {
			registry.add("spring.datasource.url", container::getJdbcUrl);
			registry.add("spring.datasource.password", container::getPassword);
			registry.add("spring.datasource.username", container::getUsername);
		}

	}

}
