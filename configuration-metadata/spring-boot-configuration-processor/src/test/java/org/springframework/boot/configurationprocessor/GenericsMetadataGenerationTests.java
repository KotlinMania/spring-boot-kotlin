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
import io.github.kotlinmania.spring.boot.configurationsample.generic.AbstractGenericProperties;
import io.github.kotlinmania.spring.boot.configurationsample.generic.ComplexGenericProperties;
import io.github.kotlinmania.spring.boot.configurationsample.generic.ConcreteBuilderProperties;
import io.github.kotlinmania.spring.boot.configurationsample.generic.GenericConfig;
import io.github.kotlinmania.spring.boot.configurationsample.generic.SimpleGenericProperties;
import io.github.kotlinmania.spring.boot.configurationsample.generic.UnresolvedGenericProperties;
import io.github.kotlinmania.spring.boot.configurationsample.generic.UpperBoundGenericPojo;
import io.github.kotlinmania.spring.boot.configurationsample.generic.WildcardConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for generics handling.
 *
 * @author Stephane Nicoll
 */
class GenericsMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	void simpleGenericProperties() {
		ConfigurationMetadata metadata = compile(AbstractGenericProperties.class, SimpleGenericProperties.class);
		assertThat(metadata).has(Metadata.withGroup("generic").fromSource(SimpleGenericProperties.class));
		assertThat(metadata).has(Metadata.withProperty("generic.name", String.class)
			.fromSource(SimpleGenericProperties.class)
			.withDescription("Generic name.")
			.withDefaultValue(null));
		assertThat(metadata)
			.has(Metadata.withProperty("generic.mappings", "java.util.Map<java.lang.Integer,java.time.Duration>")
				.fromSource(SimpleGenericProperties.class)
				.withDescription("Generic mappings.")
				.withDefaultValue(null));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void complexGenericProperties() {
		ConfigurationMetadata metadata = compile(ComplexGenericProperties.class);
		assertThat(metadata).has(Metadata.withGroup("generic").fromSource(ComplexGenericProperties.class));
		assertThat(metadata).has(Metadata.withGroup("generic.test")
			.ofType(UpperBoundGenericPojo.class)
			.fromSource(ComplexGenericProperties.class));
		assertThat(metadata)
			.has(Metadata.withProperty("generic.test.mappings", "java.util.Map<java.lang.Enum<T>,java.lang.String>")
				.fromSource(UpperBoundGenericPojo.class));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void unresolvedGenericProperties() {
		ConfigurationMetadata metadata = compile(AbstractGenericProperties.class, UnresolvedGenericProperties.class);
		assertThat(metadata).has(Metadata.withGroup("generic").fromSource(UnresolvedGenericProperties.class));
		assertThat(metadata).has(Metadata.withProperty("generic.name", String.class)
			.fromSource(UnresolvedGenericProperties.class)
			.withDescription("Generic name.")
			.withDefaultValue(null));
		assertThat(metadata)
			.has(Metadata.withProperty("generic.mappings", "java.util.Map<java.lang.Number,java.lang.Object>")
				.fromSource(UnresolvedGenericProperties.class)
				.withDescription("Generic mappings.")
				.withDefaultValue(null));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void genericTypes() {
		ConfigurationMetadata metadata = compile(GenericConfig.class);
		assertThat(metadata).has(Metadata.withGroup("generic")
			.ofType("io.github.kotlinmania.spring.boot.configurationsample.generic.GenericConfig"));
		assertThat(metadata).has(Metadata.withGroup("generic.foo")
			.ofType("io.github.kotlinmania.spring.boot.configurationsample.generic.GenericConfig$Foo"));
		assertThat(metadata).has(Metadata.withGroup("generic.foo.bar")
			.ofType("io.github.kotlinmania.spring.boot.configurationsample.generic.GenericConfig$Bar"));
		assertThat(metadata).has(Metadata.withGroup("generic.foo.bar.biz")
			.ofType("io.github.kotlinmania.spring.boot.configurationsample.generic.GenericConfig$Bar$Biz"));
		assertThat(metadata)
			.has(Metadata.withProperty("generic.foo.name").ofType(String.class).fromSource(GenericConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.string-to-bar")
			.ofType("java.util.Map<java.lang.String,io.github.kotlinmania.spring.boot.configurationsample.generic.GenericConfig$Bar<java.lang.Integer>>")
			.fromSource(GenericConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.string-to-integer")
			.ofType("java.util.Map<java.lang.String,java.lang.Integer>")
			.fromSource(GenericConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.bar.name")
			.ofType("java.lang.String")
			.fromSource(GenericConfig.Bar.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.bar.biz.name")
			.ofType("java.lang.String")
			.fromSource(GenericConfig.Bar.Biz.class));
		assertThat(metadata.getItems()).hasSize(9);
	}

	@Test
	void wildcardTypes() {
		ConfigurationMetadata metadata = compile(WildcardConfig.class);
		assertThat(metadata).has(Metadata.withGroup("wildcard").ofType(WildcardConfig.class));
		assertThat(metadata).has(Metadata.withProperty("wildcard.string-to-number")
			.ofType("java.util.Map<java.lang.String,? extends java.lang.Number>")
			.fromSource(WildcardConfig.class));
		assertThat(metadata).has(Metadata.withProperty("wildcard.integers")
			.ofType("java.util.List<? super java.lang.Integer>")
			.fromSource(WildcardConfig.class));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void builderPatternWithGenericReturnType() {
		ConfigurationMetadata metadata = compile(ConcreteBuilderProperties.class);
		assertThat(metadata).has(Metadata.withGroup("builder").fromSource(ConcreteBuilderProperties.class));
		assertThat(metadata)
			.has(Metadata.withProperty("builder.number", Integer.class).fromSource(ConcreteBuilderProperties.class));
		assertThat(metadata).has(
				Metadata.withProperty("builder.description", String.class).fromSource(ConcreteBuilderProperties.class));
		assertThat(metadata.getItems()).hasSize(3);
	}

}
