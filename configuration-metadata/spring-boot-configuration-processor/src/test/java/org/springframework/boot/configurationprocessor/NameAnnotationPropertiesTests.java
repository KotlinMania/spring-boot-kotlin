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

package io.github.kotlinmania.spring.boot.configurationprocessor;

import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.configurationprocessor.metadata.ConfigurationMetadata;
import io.github.kotlinmania.spring.boot.configurationprocessor.metadata.Metadata;
import io.github.kotlinmania.spring.boot.configurationsample.name.ConstructorParameterNameAnnotationProperties;
import io.github.kotlinmania.spring.boot.configurationsample.name.DuplicateNameConstructorParameterProperties;
import io.github.kotlinmania.spring.boot.configurationsample.name.DuplicateNameJavaBeanProperties;
import io.github.kotlinmania.spring.boot.configurationsample.name.JavaBeanNameAnnotationProperties;
import io.github.kotlinmania.spring.boot.configurationsample.name.LombokNameAnnotationProperties;
import io.github.kotlinmania.spring.boot.configurationsample.name.RecordComponentNameAnnotationProperties;
import org.springframework.core.test.tools.CompilationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Metadata generation tests for using {@code @Name}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class NameAnnotationPropertiesTests extends AbstractMetadataGenerationTests {

	@Test
	void constructorParameterNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(ConstructorParameterNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(ConstructorParameterNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(ConstructorParameterNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void recordComponentNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(RecordComponentNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(RecordComponentNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(RecordComponentNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void javaBeanNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(JavaBeanNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(JavaBeanNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(JavaBeanNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void lombokNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(LombokNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(LombokNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(LombokNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void duplicateNameOnJavaBeanPropertiesFailsCompilation() {
		assertThatExceptionOfType(CompilationException.class)
			.isThrownBy(() -> compile(DuplicateNameJavaBeanProperties.class))
			.withMessageContaining("Unable to compile source")
			.withMessageContaining("Property name 'aaa' maps to distinct properties in type ")
			.withMessageContaining(DuplicateNameJavaBeanProperties.class.getName());
	}

	@Test
	void duplicateNameOnConstructorParametersFailsCompilation() {
		assertThatExceptionOfType(CompilationException.class)
			.isThrownBy(() -> compile(DuplicateNameConstructorParameterProperties.class))
			.withMessageContaining("Unable to compile source")
			.withMessageContaining("Property name 'aaa' maps to distinct properties in type ")
			.withMessageContaining(DuplicateNameConstructorParameterProperties.class.getName());
	}

}
