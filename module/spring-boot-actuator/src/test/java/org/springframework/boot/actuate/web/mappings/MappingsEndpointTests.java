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

package io.github.kotlinmania.spring.boot.actuate.web.mappings;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.actuate.web.mappings.MappingsEndpoint.ApplicationMappingsDescriptor;
import io.github.kotlinmania.spring.boot.actuate.web.mappings.MappingsEndpoint.ContextMappingsDescriptor;
import io.github.kotlinmania.spring.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappingsEndpoint}.
 *
 * @author Lee JiWon
 */
class MappingsEndpointTests {

	@Test
	void mappingsParentIdMatchesParentContextId() {
		ApplicationContextRunner parentRunner = new ApplicationContextRunner();
		parentRunner.run((parent) -> new ApplicationContextRunner().withUserConfiguration(EndpointConfiguration.class)
			.withParent(parent)
			.run((child) -> {
				ApplicationMappingsDescriptor result = child.getBean(MappingsEndpoint.class).mappings();
				ContextMappingsDescriptor parentDescriptor = result.getContexts().get(parent.getId());
				ContextMappingsDescriptor childDescriptor = result.getContexts().get(child.getId());
				assertThat(parentDescriptor).isNotNull();
				assertThat(parentDescriptor.getParentId()).isNull();
				assertThat(childDescriptor).isNotNull();
				assertThat(childDescriptor.getParentId()).isEqualTo(parent.getId());
			}));
	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointConfiguration {

		@Bean
		MappingsEndpoint mappingsEndpoint(ConfigurableApplicationContext context) {
			return new MappingsEndpoint(Collections.emptyList(), context);
		}

	}

}
