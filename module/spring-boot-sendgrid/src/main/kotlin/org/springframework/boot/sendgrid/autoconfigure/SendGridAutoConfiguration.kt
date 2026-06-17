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

package org.springframework.boot.sendgrid.autoconfigure

import com.sendgrid.Client
import com.sendgrid.SendGrid
import com.sendgrid.SendGridAPI
import org.apache.http.HttpHost
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * [Auto-configuration][EnableAutoConfiguration] for SendGrid.
 *
 * @author Maciej Walkowiak
 * @author Patrick Bray
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(SendGrid::class)
@ConditionalOnProperty("spring.sendgrid.api-key")
@EnableConfigurationProperties(SendGridProperties::class)
class SendGridAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(SendGridAPI::class)
	fun sendGrid(properties: SendGridProperties): SendGrid {
		val proxy = properties.proxy
		val host = proxy?.host
		val port = proxy?.port
		if (host != null && port != null) {
			val httpProxy = HttpHost(host, port)
			return SendGrid(properties.apiKey, Client(HttpClientBuilder.create().setProxy(httpProxy).build()))
		}
		return SendGrid(properties.apiKey)
	}

}
