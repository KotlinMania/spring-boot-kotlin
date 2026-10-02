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

package io.github.kotlinmania.spring.boot.autoconfigureprocessor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * Version of {@link AutoConfigureAnnotationProcessor} used for testing.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 */
@SupportedAnnotationTypes({ "io.github.kotlinmania.spring.boot.autoconfigureprocessor.TestConditionalOnClass",
		"io.github.kotlinmania.spring.boot.autoconfigureprocessor.TestConditionalOnBean",
		"io.github.kotlinmania.spring.boot.autoconfigureprocessor.TestConditionalOnSingleCandidate",
		"io.github.kotlinmania.spring.boot.autoconfigureprocessor.TestConditionalOnWebApplication",
		"io.github.kotlinmania.spring.boot.autoconfigureprocessor.TestAutoConfigureBefore",
		"io.github.kotlinmania.spring.boot.autoconfigureprocessor.TestAutoConfigureAfter",
		"io.github.kotlinmania.spring.boot.autoconfigureprocessor.TestAutoConfigureOrder",
		"io.github.kotlinmania.spring.boot.autoconfigureprocessor.TestAutoConfiguration" })
public class TestAutoConfigureAnnotationProcessor extends AutoConfigureAnnotationProcessor {

	public TestAutoConfigureAnnotationProcessor() {
	}

	@Override
	protected List<PropertyGenerator> getPropertyGenerators() {
		List<PropertyGenerator> generators = new ArrayList<>();
		String annotationPackage = "io.github.kotlinmania.spring.boot.autoconfigureprocessor";
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnClass")
			.withAnnotation("TestConditionalOnClass", new OnClassConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnBean")
			.withAnnotation("TestConditionalOnBean", new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnSingleCandidate")
			.withAnnotation("TestConditionalOnSingleCandidate", new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnWebApplication")
			.withAnnotation("TestConditionalOnWebApplication", ValueExtractor.allFrom("type")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureBefore", true)
			.withAnnotation("TestAutoConfigureBefore", ValueExtractor.allFrom("value", "name"))
			.withAnnotation("TestAutoConfiguration", ValueExtractor.allFrom("before", "beforeName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureAfter", true)
			.withAnnotation("TestAutoConfigureAfter", ValueExtractor.allFrom("value", "name"))
			.withAnnotation("TestAutoConfiguration", ValueExtractor.allFrom("after", "afterName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureOrder")
			.withAnnotation("TestAutoConfigureOrder", ValueExtractor.allFrom("value")));
		return generators;
	}

}
