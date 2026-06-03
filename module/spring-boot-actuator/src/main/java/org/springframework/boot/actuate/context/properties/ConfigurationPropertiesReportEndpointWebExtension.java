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

package io.github.kotlinmania.spring.boot.actuate.context.properties;

import java.util.Set;

import io.github.kotlinmania.spring.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor;
import io.github.kotlinmania.spring.boot.actuate.endpoint.SecurityContext;
import io.github.kotlinmania.spring.boot.actuate.endpoint.Show;
import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.ReadOperation;
import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.Selector;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebEndpointResponse;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.EndpointWebExtension;

/**
 * {@link EndpointWebExtension @EndpointWebExtension} for the
 * {@link ConfigurationPropertiesReportEndpoint}.
 *
 * @author Chris Bono
 * @since 2.5.0
 */
@EndpointWebExtension(endpoint = ConfigurationPropertiesReportEndpoint.class)
public class ConfigurationPropertiesReportEndpointWebExtension {

	private final ConfigurationPropertiesReportEndpoint delegate;

	private final Show showValues;

	private final Set<String> roles;

	public ConfigurationPropertiesReportEndpointWebExtension(ConfigurationPropertiesReportEndpoint delegate,
			Show showValues, Set<String> roles) {
		this.delegate = delegate;
		this.showValues = showValues;
		this.roles = roles;
	}

	@ReadOperation
	public ConfigurationPropertiesDescriptor configurationProperties(SecurityContext securityContext) {
		boolean showUnsanitized = this.showValues.isShown(securityContext, this.roles);
		return this.delegate.getConfigurationProperties(showUnsanitized);
	}

	@ReadOperation
	public WebEndpointResponse<ConfigurationPropertiesDescriptor> configurationPropertiesWithPrefix(
			SecurityContext securityContext, @Selector String prefix) {
		boolean showUnsanitized = this.showValues.isShown(securityContext, this.roles);
		ConfigurationPropertiesDescriptor configurationProperties = this.delegate.getConfigurationProperties(prefix,
				showUnsanitized);
		boolean foundMatchingBeans = configurationProperties.getContexts()
			.values()
			.stream()
			.anyMatch((context) -> !context.getBeans().isEmpty());
		return (foundMatchingBeans) ? new WebEndpointResponse<>(configurationProperties, WebEndpointResponse.STATUS_OK)
				: new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND);
	}

}
