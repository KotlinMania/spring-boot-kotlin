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

package io.github.kotlinmania.spring.boot.grpc.client.autoconfigure;

import io.grpc.stub.AbstractStub;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.grpc.client.CompositeGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a
 * {@link CompositeGrpcChannelFactory}.
 *
 * @author Chris Bono
 * @since 4.1.0
 */
@AutoConfiguration
@ConditionalOnClass({ AbstractStub.class, GrpcChannelBuilderCustomizer.class })
@ConditionalOnProperty(name = "spring.grpc.client.enabled", matchIfMissing = true)
@Conditional(CompositeChannelFactoryAutoConfiguration.MultipleNonPrimaryChannelFactoriesCondition.class)
public final class CompositeChannelFactoryAutoConfiguration {

	@Bean
	@Primary
	CompositeGrpcChannelFactory compositeChannelFactory(ObjectProvider<GrpcChannelFactory> channelFactoriesProvider) {
		return new CompositeGrpcChannelFactory(channelFactoriesProvider.orderedStream().toList());
	}

	static class MultipleNonPrimaryChannelFactoriesCondition extends NoneNestedConditions {

		MultipleNonPrimaryChannelFactoriesCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnMissingBean(GrpcChannelFactory.class)
		static class NoChannelFactoryCondition {

		}

		@ConditionalOnSingleCandidate(GrpcChannelFactory.class)
		static class SingleInjectableChannelFactoryCondition {

		}

	}

}
