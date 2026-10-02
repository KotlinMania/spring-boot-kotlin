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

package io.github.kotlinmania.spring.boot.docs.features.externalconfig.typesafeconfigurationproperties.conversion.durations.constructorbinding;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.github.kotlinmania.spring.boot.context.properties.ConfigurationProperties;
import io.github.kotlinmania.spring.boot.context.properties.bind.DefaultValue;
import io.github.kotlinmania.spring.boot.convert.DurationUnit;

@ConfigurationProperties("my")
public class MyProperties {

	// @fold:on // fields...
	private final Duration sessionTimeout;

	private final Duration readTimeout;

	// @fold:off
	public MyProperties(@DurationUnit(ChronoUnit.SECONDS) @DefaultValue("30s") Duration sessionTimeout,
			@DefaultValue("1000ms") Duration readTimeout) {
		this.sessionTimeout = sessionTimeout;
		this.readTimeout = readTimeout;
	}

	// @fold:on // getters...
	public Duration getSessionTimeout() {
		return this.sessionTimeout;
	}

	public Duration getReadTimeout() {
		return this.readTimeout;
	}
	// @fold:off

}
