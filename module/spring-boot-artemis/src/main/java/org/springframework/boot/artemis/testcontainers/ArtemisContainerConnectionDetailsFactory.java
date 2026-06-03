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

package io.github.kotlinmania.spring.boot.artemis.testcontainers;

import org.jspecify.annotations.Nullable;
import org.testcontainers.activemq.ArtemisContainer;

import io.github.kotlinmania.spring.boot.artemis.autoconfigure.ArtemisConnectionDetails;
import io.github.kotlinmania.spring.boot.artemis.autoconfigure.ArtemisMode;
import io.github.kotlinmania.spring.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import io.github.kotlinmania.spring.boot.testcontainers.service.connection.ContainerConnectionSource;
import io.github.kotlinmania.spring.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link ArtemisConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated {@link ArtemisContainer}.
 *
 * @author Eddú Meléndez
 */
class ArtemisContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ArtemisContainer, ArtemisConnectionDetails> {

	@Override
	protected ArtemisConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ArtemisContainer> source) {
		return new ArtemisContainerConnectionDetails(source);
	}

	private static final class ArtemisContainerConnectionDetails extends ContainerConnectionDetails<ArtemisContainer>
			implements ArtemisConnectionDetails {

		private ArtemisContainerConnectionDetails(ContainerConnectionSource<ArtemisContainer> source) {
			super(source);
		}

		@Override
		public ArtemisMode getMode() {
			return ArtemisMode.NATIVE;
		}

		@Override
		public String getBrokerUrl() {
			return getContainer().getBrokerUrl();
		}

		@Override
		public @Nullable String getUser() {
			return getContainer().getUser();
		}

		@Override
		public @Nullable String getPassword() {
			return getContainer().getPassword();
		}

	}

}
