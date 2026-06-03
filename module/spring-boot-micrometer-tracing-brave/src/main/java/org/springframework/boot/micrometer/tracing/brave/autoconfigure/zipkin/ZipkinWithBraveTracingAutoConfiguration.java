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

package io.github.kotlinmania.spring.boot.micrometer.tracing.brave.autoconfigure.zipkin;

import brave.Tag;
import brave.Tags;
import brave.handler.MutableSpan;
import zipkin2.reporter.BytesEncoder;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.MutableSpanBytesEncoder;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.micrometer.tracing.autoconfigure.ConditionalOnEnabledTracingExport;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Zipkin tracing with Brave.
 *
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 * @author Wick Dynex
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(afterName = "io.github.kotlinmania.spring.boot.zipkin.autoconfigure.ZipkinAutoConfiguration")
@ConditionalOnClass({ Encoding.class, AsyncZipkinSpanHandler.class })
public final class ZipkinWithBraveTracingAutoConfiguration {

	@Bean
	@ConditionalOnBean(Encoding.class)
	@ConditionalOnMissingBean(value = MutableSpan.class, parameterizedContainer = BytesEncoder.class)
	BytesEncoder<MutableSpan> mutableSpanBytesEncoder(Encoding encoding,
			ObjectProvider<Tag<Throwable>> throwableTagProvider) {
		Tag<Throwable> throwableTag = throwableTagProvider.getIfAvailable(() -> Tags.ERROR);
		return MutableSpanBytesEncoder.create(encoding, throwableTag);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(BytesMessageSender.class)
	@ConditionalOnEnabledTracingExport("zipkin")
	AsyncZipkinSpanHandler asyncZipkinSpanHandler(BytesMessageSender sender,
			BytesEncoder<MutableSpan> mutableSpanBytesEncoder) {
		return AsyncZipkinSpanHandler.newBuilder(sender).build(mutableSpanBytesEncoder);
	}

}
