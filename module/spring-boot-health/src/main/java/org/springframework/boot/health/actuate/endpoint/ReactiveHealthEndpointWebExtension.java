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

package io.github.kotlinmania.spring.boot.health.actuate.endpoint;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import io.github.kotlinmania.spring.boot.actuate.endpoint.ApiVersion;
import io.github.kotlinmania.spring.boot.actuate.endpoint.SecurityContext;
import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.ReadOperation;
import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.Selector;
import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.Selector.Match;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebEndpointResponse;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.WebServerNamespace;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import io.github.kotlinmania.spring.boot.health.contributor.Health;
import io.github.kotlinmania.spring.boot.health.registry.HealthContributorRegistry;
import io.github.kotlinmania.spring.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Reactive {@link EndpointWebExtension @EndpointWebExtension} for the
 * {@link HealthEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0.0
 */
@EndpointWebExtension(endpoint = HealthEndpoint.class)
@ImportRuntimeHints(HealthEndpointWebExtensionRuntimeHints.class)
public class ReactiveHealthEndpointWebExtension
		extends HealthEndpointSupport<Mono<? extends Health>, Mono<? extends HealthDescriptor>> {

	/**
	 * Create a new {@link ReactiveHealthEndpointWebExtension} instance.
	 * @param registry the health contributor registry
	 * @param fallbackRegistry the fallback registry or {@code null}
	 * @param groups the health endpoint groups
	 * @param slowContributorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 */
	public ReactiveHealthEndpointWebExtension(ReactiveHealthContributorRegistry registry,
			@Nullable HealthContributorRegistry fallbackRegistry, HealthEndpointGroups groups,
			@Nullable Duration slowContributorLoggingThreshold) {
		super(Contributor.reactive(registry, fallbackRegistry), groups, slowContributorLoggingThreshold);
	}

	@ReadOperation
	public Mono<WebEndpointResponse<? extends HealthDescriptor>> health(ApiVersion apiVersion,
			@Nullable WebServerNamespace serverNamespace, SecurityContext securityContext) {
		return health(apiVersion, serverNamespace, securityContext, false, EMPTY_PATH);
	}

	@ReadOperation
	public Mono<WebEndpointResponse<? extends HealthDescriptor>> health(ApiVersion apiVersion,
			@Nullable WebServerNamespace serverNamespace, SecurityContext securityContext,
			@Selector(match = Match.ALL_REMAINING) String... path) {
		return health(apiVersion, serverNamespace, securityContext, false, path);
	}

	public Mono<WebEndpointResponse<? extends HealthDescriptor>> health(ApiVersion apiVersion,
			@Nullable WebServerNamespace serverNamespace, SecurityContext securityContext, boolean showAll,
			String... path) {
		Result<Mono<? extends HealthDescriptor>> result = getResult(apiVersion, serverNamespace, securityContext,
				showAll, path);
		if (result == null) {
			return (Arrays.equals(path, EMPTY_PATH))
					? Mono.just(new WebEndpointResponse<>(IndicatedHealthDescriptor.UP, WebEndpointResponse.STATUS_OK))
					: Mono.just(new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND));
		}
		HealthEndpointGroup group = result.group();
		return result.descriptor().map((health) -> {
			int statusCode = group.getHttpCodeStatusMapper().getStatusCode(health.getStatus());
			return new WebEndpointResponse<>(health, statusCode);
		});
	}

	@Override
	protected Mono<? extends HealthDescriptor> aggregateDescriptors(ApiVersion apiVersion,
			Map<String, Mono<? extends HealthDescriptor>> contributions, StatusAggregator statusAggregator,
			boolean showComponents, @Nullable Set<String> groupNames) {
		return Flux.fromIterable(contributions.entrySet())
			.flatMap(NamedHealthDescriptor::create)
			.collectMap(NamedHealthDescriptor::name, NamedHealthDescriptor::descriptor)
			.map((components) -> this.getCompositeDescriptor(apiVersion, components, statusAggregator, showComponents,
					groupNames));
	}

	/**
	 * A named {@link HealthDescriptor}.
	 */
	private record NamedHealthDescriptor(String name, HealthDescriptor descriptor) {

		static Mono<NamedHealthDescriptor> create(Map.Entry<String, Mono<? extends HealthDescriptor>> entry) {
			Mono<String> name = Mono.just(entry.getKey());
			Mono<? extends HealthDescriptor> health = entry.getValue();
			return Mono.zip(NamedHealthDescriptor::ofPair, name, health);
		}

		private static NamedHealthDescriptor ofPair(Object... pair) {
			return new NamedHealthDescriptor((String) pair[0], (HealthDescriptor) pair[1]);
		}

	}

}
