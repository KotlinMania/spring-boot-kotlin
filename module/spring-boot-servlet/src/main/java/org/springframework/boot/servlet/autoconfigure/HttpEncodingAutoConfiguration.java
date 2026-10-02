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

package io.github.kotlinmania.spring.boot.servlet.autoconfigure;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.servlet.filter.OrderedCharacterEncodingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for configuring the encoding to use
 * in Servlet web applications.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ServletEncodingProperties.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(CharacterEncodingFilter.class)
@ConditionalOnBooleanProperty(name = "spring.servlet.encoding.enabled", matchIfMissing = true)
public final class HttpEncodingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	CharacterEncodingFilter characterEncodingFilter(ServletEncodingProperties properties) {
		CharacterEncodingFilter filter = new OrderedCharacterEncodingFilter();
		filter.setEncoding(properties.getCharset().name());
		filter.setForceRequestEncoding(properties.shouldForce(ServletEncodingProperties.HttpMessageType.REQUEST));
		filter.setForceResponseEncoding(properties.shouldForce(ServletEncodingProperties.HttpMessageType.RESPONSE));
		return filter;
	}

}
