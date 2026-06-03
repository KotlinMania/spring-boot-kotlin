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

package io.github.kotlinmania.spring.boot.webflux.autoconfigure.actuate.web.mappings;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.web.mappings.MappingDescriptionProvider;
import io.github.kotlinmania.spring.boot.actuate.web.mappings.MappingsEndpoint;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.webflux.actuate.web.mappings.DispatcherHandlersMappingDescriptionProvider;
import io.github.kotlinmania.spring.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to {@link MappingDescriptionProvider
 * describe} WebFlux mappings.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(after = WebFluxAutoConfiguration.class)
@ConditionalOnClass({ ConditionalOnAvailableEndpoint.class, DispatcherHandler.class, MappingsEndpoint.class })
@ConditionalOnAvailableEndpoint(MappingsEndpoint.class)
@ConditionalOnBean(DispatcherHandler.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
public final class WebFluxMappingsAutoConfiguration {

	@Bean
	DispatcherHandlersMappingDescriptionProvider dispatcherHandlersMappingDescriptionProvider() {
		return new DispatcherHandlersMappingDescriptionProvider();
	}

}
