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

package io.github.kotlinmania.spring.boot.restclient.autoconfigure;

import java.util.function.Consumer;

import io.github.kotlinmania.spring.boot.http.client.ClientHttpRequestFactoryBuilder;
import io.github.kotlinmania.spring.boot.http.client.HttpClientSettings;
import io.github.kotlinmania.spring.boot.ssl.SslBundle;
import io.github.kotlinmania.spring.boot.ssl.SslBundles;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * An auto-configured {@link RestClientSsl} implementation.
 *
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
class AutoConfiguredRestClientSsl implements RestClientSsl {

	private final ClientHttpRequestFactoryBuilder<?> builder;

	private final HttpClientSettings settings;

	private final SslBundles sslBundles;

	AutoConfiguredRestClientSsl(ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder,
			HttpClientSettings settings, SslBundles sslBundles) {
		this.builder = clientHttpRequestFactoryBuilder;
		this.settings = settings;
		this.sslBundles = sslBundles;
	}

	@Override
	public Consumer<RestClient.Builder> fromBundle(String bundleName) {
		return fromBundle(this.sslBundles.getBundle(bundleName));
	}

	@Override
	public Consumer<RestClient.Builder> fromBundle(SslBundle bundle) {
		return (builder) -> builder.requestFactory(requestFactory(bundle));
	}

	private ClientHttpRequestFactory requestFactory(SslBundle bundle) {
		return this.builder.build(this.settings.withSslBundle(bundle));
	}

}
