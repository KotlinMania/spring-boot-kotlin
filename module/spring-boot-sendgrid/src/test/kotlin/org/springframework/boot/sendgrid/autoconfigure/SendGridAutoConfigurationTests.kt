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

import com.sendgrid.SendGrid
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Tests for [SendGridAutoConfiguration].
 *
 * @author Maciej Walkowiak
 * @author Patrick Bray
 */
class SendGridAutoConfigurationTests {

	private var context: AnnotationConfigApplicationContext? = null

	@AfterEach
	fun close() {
		context?.close()
	}

	@Test
	fun expectedSendGridBeanCreatedApiKey() {
		val context = loadContext("spring.sendgrid.api-key:SG.SECRET-API-KEY")
		val sendGrid = context.getBean(SendGrid::class.java)
		assertThat(sendGrid.requestHeaders).containsEntry("Authorization", "Bearer SG.SECRET-API-KEY")
	}

	@Test
	fun autoConfigurationNotFiredWhenPropertiesNotSet() {
		val context = loadContext()
		assertThatExceptionOfType(NoSuchBeanDefinitionException::class.java)
			.isThrownBy { context.getBean(SendGrid::class.java) }
	}

	@Test
	fun autoConfigurationNotFiredWhenBeanAlreadyCreated() {
		val context = loadContext(ManualSendGridConfiguration::class.java, "spring.sendgrid.api-key:SG.SECRET-API-KEY")
		val sendGrid = context.getBean(SendGrid::class.java)
		assertThat(sendGrid.requestHeaders).containsEntry("Authorization", "Bearer SG.CUSTOM_API_KEY")
	}

	@Test
	fun expectedSendGridBeanWithProxyCreated() {
		val context = loadContext("spring.sendgrid.api-key:SG.SECRET-API-KEY", "spring.sendgrid.proxy.host:localhost",
			"spring.sendgrid.proxy.port:5678")
		val sendGrid = context.getBean(SendGrid::class.java)
		assertThat(sendGrid).extracting("client.httpClient.routePlanner")
			.isInstanceOf(DefaultProxyRoutePlanner::class.java)
	}

	private fun loadContext(vararg environment: String): AnnotationConfigApplicationContext {
		return loadContext(null, *environment)
	}

	private fun loadContext(additionalConfiguration: Class<*>?,
			vararg environment: String): AnnotationConfigApplicationContext {
		val context = AnnotationConfigApplicationContext()
		TestPropertyValues.of(*environment).applyTo(context)
		ConfigurationPropertySources.attach(context.environment)
		context.register(SendGridAutoConfiguration::class.java)
		if (additionalConfiguration != null) {
			context.register(additionalConfiguration)
		}
		context.refresh()
		this.context = context
		return context
	}

	@Configuration(proxyBeanMethods = false)
	internal class ManualSendGridConfiguration {

		@Bean
		fun sendGrid(): SendGrid {
			return SendGrid("SG.CUSTOM_API_KEY", true)
		}

	}

}
