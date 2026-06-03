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

package io.github.kotlinmania.spring.boot.actuate.autoconfigure.audit;

import io.github.kotlinmania.spring.boot.actuate.audit.AuditEvent;
import io.github.kotlinmania.spring.boot.actuate.audit.AuditEventRepository;
import io.github.kotlinmania.spring.boot.actuate.audit.listener.AbstractAuditListener;
import io.github.kotlinmania.spring.boot.actuate.audit.listener.AuditListener;
import io.github.kotlinmania.spring.boot.actuate.security.AbstractAuthenticationAuditListener;
import io.github.kotlinmania.spring.boot.actuate.security.AbstractAuthorizationAuditListener;
import io.github.kotlinmania.spring.boot.actuate.security.AuthenticationAuditListener;
import io.github.kotlinmania.spring.boot.actuate.security.AuthorizationAuditListener;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link AuditEvent}s.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnBean(AuditEventRepository.class)
@ConditionalOnBooleanProperty(name = "management.auditevents.enabled", matchIfMissing = true)
public final class AuditAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AbstractAuditListener.class)
	AuditListener auditListener(AuditEventRepository auditEventRepository) {
		return new AuditListener(auditEventRepository);
	}

	@Bean
	@ConditionalOnMissingBean(AbstractAuthenticationAuditListener.class)
	AuthenticationAuditListener authenticationAuditListener() {
		return new AuthenticationAuditListener();
	}

	@Bean
	@ConditionalOnMissingBean(AbstractAuthorizationAuditListener.class)
	AuthorizationAuditListener authorizationAuditListener() {
		return new AuthorizationAuditListener();
	}

}
