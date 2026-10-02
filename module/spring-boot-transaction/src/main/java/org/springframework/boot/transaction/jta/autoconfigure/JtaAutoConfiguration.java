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

package io.github.kotlinmania.spring.boot.transaction.jta.autoconfigure;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import io.github.kotlinmania.spring.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JTA.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Nishant Raut
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { TransactionAutoConfiguration.class, TransactionManagerCustomizationAutoConfiguration.class })
@ConditionalOnClass(jakarta.transaction.Transaction.class)
@ConditionalOnBooleanProperty(name = "spring.jta.enabled", matchIfMissing = true)
@Import(JndiJtaConfiguration.class)
public final class JtaAutoConfiguration {

}
