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

package io.github.kotlinmania.spring.boot.netty.autoconfigure;

import io.netty.util.NettyRuntime;
import io.netty.util.ResourceLeakDetector;

import io.github.kotlinmania.spring.boot.LazyInitializationExcludeFilter;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Netty.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(NettyRuntime.class)
@EnableConfigurationProperties(NettyProperties.class)
public final class NettyAutoConfiguration {

	NettyAutoConfiguration(NettyProperties properties) {
		if (properties.getLeakDetection() != null) {
			NettyProperties.LeakDetection leakDetection = properties.getLeakDetection();
			ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.valueOf(leakDetection.name()));
		}
	}

	@Bean
	static LazyInitializationExcludeFilter nettyAutoConfigurationLazyInitializationExcludeFilter() {
		return LazyInitializationExcludeFilter.forBeanTypes(NettyAutoConfiguration.class);
	}

}
