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

package io.github.kotlinmania.spring.boot.webmvc.autoconfigure.actuate.web.mappings;

import io.github.kotlinmania.spring.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import io.github.kotlinmania.spring.boot.actuate.web.mappings.MappingDescriptionProvider;
import io.github.kotlinmania.spring.boot.actuate.web.mappings.MappingsEndpoint;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.webmvc.actuate.web.mappings.DispatcherServletsMappingDescriptionProvider;
import io.github.kotlinmania.spring.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to describe Web MVC
 * {@link MappingDescriptionProvider mappings}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(after = DispatcherServletAutoConfiguration.class)
@ConditionalOnClass({ ConditionalOnAvailableEndpoint.class, DispatcherServlet.class, MappingsEndpoint.class })
@ConditionalOnAvailableEndpoint(MappingsEndpoint.class)
@ConditionalOnBean(DispatcherServlet.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public final class WebMvcMappingsAutoConfiguration {

	@Bean
	DispatcherServletsMappingDescriptionProvider dispatcherServletMappingDescriptionProvider() {
		return new DispatcherServletsMappingDescriptionProvider();
	}

}
