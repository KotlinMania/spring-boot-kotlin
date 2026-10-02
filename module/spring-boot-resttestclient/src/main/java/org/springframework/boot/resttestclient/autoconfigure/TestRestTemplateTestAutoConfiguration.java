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

package io.github.kotlinmania.spring.boot.resttestclient.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.restclient.RestTemplateBuilder;
import io.github.kotlinmania.spring.boot.resttestclient.TestRestTemplate;
import io.github.kotlinmania.spring.boot.resttestclient.TestRestTemplate.HttpClientOption;
import io.github.kotlinmania.spring.boot.test.http.server.LocalTestWebServer;
import io.github.kotlinmania.spring.boot.test.http.server.LocalTestWebServer.Scheme;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Test auto-configuration for {@link TestRestTemplate}.
 *
 * @author Andy Wilkinson
 * @see AutoConfigureTestRestTemplate
 */
@AutoConfiguration
final class TestRestTemplateTestAutoConfiguration {

	@Bean(name = "io.github.kotlinmania.spring.boot.resttestclient.TestRestTemplate")
	@ConditionalOnMissingBean
	TestRestTemplate testRestTemplate(ObjectProvider<RestTemplateBuilder> builderProvider,
			ApplicationContext applicationContext) {
		RestTemplateBuilder builder = builderProvider.getIfAvailable(RestTemplateBuilder::new);
		LocalTestWebServer localTestWebServer = LocalTestWebServer.obtain(applicationContext);
		TestRestTemplate template = new TestRestTemplate(builder, null, null,
				httpClientOptions(localTestWebServer.scheme()));
		template.setUriTemplateHandler(localTestWebServer.uriBuilderFactory());
		return template;
	}

	private HttpClientOption[] httpClientOptions(Scheme scheme) {
		return switch (scheme) {
			case HTTP -> new HttpClientOption[] {};
			case HTTPS -> new HttpClientOption[] { HttpClientOption.SSL };
		};
	}

}
