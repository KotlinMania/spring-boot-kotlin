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

package io.github.kotlinmania.spring.boot.autoconfigure.ssl;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.io.ApplicationResourceLoader;
import io.github.kotlinmania.spring.boot.ssl.DefaultSslBundleRegistry;
import io.github.kotlinmania.spring.boot.ssl.SslBundleRegistry;
import io.github.kotlinmania.spring.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SSL.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SslProperties.class)
public final class SslAutoConfiguration {

	private final ResourceLoader resourceLoader;

	private final SslProperties sslProperties;

	SslAutoConfiguration(ResourceLoader resourceLoader, SslProperties sslProperties) {
		this.resourceLoader = ApplicationResourceLoader.get(resourceLoader, true);
		this.sslProperties = sslProperties;
	}

	@Bean
	FileWatcher fileWatcher() {
		return new FileWatcher(this.sslProperties.getBundle().getWatch().getFile().getQuietPeriod());
	}

	@Bean
	SslPropertiesBundleRegistrar sslPropertiesSslBundleRegistrar(FileWatcher fileWatcher) {
		return new SslPropertiesBundleRegistrar(this.sslProperties, fileWatcher, this.resourceLoader);
	}

	@Bean
	@ConditionalOnMissingBean({ SslBundleRegistry.class, SslBundles.class })
	DefaultSslBundleRegistry sslBundleRegistry(ObjectProvider<SslBundleRegistrar> sslBundleRegistrars) {
		DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();
		sslBundleRegistrars.orderedStream().forEach((registrar) -> registrar.registerBundles(registry));
		return registry;
	}

}
