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

package io.github.kotlinmania.spring.boot.data.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactoryBean;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data R2DBC Repositories.
 *
 * @author Mark Paluch
 * @since 4.0.0
 * @see EnableR2dbcRepositories
 */
@AutoConfiguration(after = DataR2dbcAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, R2dbcRepository.class })
@ConditionalOnBean(DatabaseClient.class)
@ConditionalOnBooleanProperty(name = "spring.data.r2dbc.repositories.enabled", matchIfMissing = true)
@ConditionalOnMissingBean(R2dbcRepositoryFactoryBean.class)
@Import(DataR2dbcRepositoriesAutoConfigureRegistrar.class)
public final class DataR2dbcRepositoriesAutoConfiguration {

}
