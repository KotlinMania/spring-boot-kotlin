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

package io.github.kotlinmania.spring.boot.graphql.autoconfigure.observation;

import graphql.GraphQL;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.observation.DataFetcherObservationConvention;
import org.springframework.graphql.observation.DataLoaderObservationConvention;
import org.springframework.graphql.observation.ExecutionRequestObservationConvention;
import org.springframework.graphql.observation.GraphQlObservationInstrumentation;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring
 * GraphQL endpoints.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "io.github.kotlinmania.spring.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnClass({ GraphQL.class, GraphQlSource.class, Observation.class })
public final class GraphQlObservationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	GraphQlObservationInstrumentation graphQlObservationInstrumentation(ObservationRegistry observationRegistry,
			ObjectProvider<ExecutionRequestObservationConvention> executionConvention,
			ObjectProvider<DataFetcherObservationConvention> dataFetcherConvention,
			ObjectProvider<DataLoaderObservationConvention> dataLoaderObservationConvention) {
		return new GraphQlObservationInstrumentation(observationRegistry, executionConvention.getIfAvailable(),
				dataFetcherConvention.getIfAvailable(), dataLoaderObservationConvention.getIfAvailable());
	}

}
