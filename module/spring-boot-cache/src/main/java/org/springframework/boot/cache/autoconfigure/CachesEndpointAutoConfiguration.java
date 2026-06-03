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

package io.github.kotlinmania.spring.boot.cache.autoconfigure;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.SimpleAutowireCandidateResolver;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.cache.actuate.endpoint.CachesEndpoint;
import io.github.kotlinmania.spring.boot.cache.actuate.endpoint.CachesEndpointWebExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link CachesEndpoint}.
 *
 * @author Johannes Edmeier
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = CacheAutoConfiguration.class)
@ConditionalOnClass({ CacheManager.class, ConditionalOnAvailableEndpoint.class })
@ConditionalOnAvailableEndpoint(CachesEndpoint.class)
public final class CachesEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	CachesEndpoint cachesEndpoint(ConfigurableListableBeanFactory beanFactory) {
		return new CachesEndpoint(
				SimpleAutowireCandidateResolver.resolveAutowireCandidates(beanFactory, CacheManager.class));
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(CachesEndpoint.class)
	@ConditionalOnAvailableEndpoint(exposure = EndpointExposure.WEB)
	CachesEndpointWebExtension cachesEndpointWebExtension(CachesEndpoint cachesEndpoint) {
		return new CachesEndpointWebExtension(cachesEndpoint);
	}

}
