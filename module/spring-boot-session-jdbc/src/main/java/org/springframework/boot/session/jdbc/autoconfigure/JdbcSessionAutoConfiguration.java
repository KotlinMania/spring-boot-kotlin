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

package io.github.kotlinmania.spring.boot.session.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.context.properties.PropertyMapper;
import io.github.kotlinmania.spring.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import io.github.kotlinmania.spring.boot.session.autoconfigure.SessionAutoConfiguration;
import io.github.kotlinmania.spring.boot.session.autoconfigure.SessionTimeout;
import io.github.kotlinmania.spring.boot.sql.autoconfigure.init.OnDatabaseInitializationCondition;
import io.github.kotlinmania.spring.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.session.jdbc.config.annotation.web.http.JdbcHttpSessionConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Session JDBC.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 4.0.0
 */
@AutoConfiguration(before = SessionAutoConfiguration.class, after = DataSourceAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Session.class, JdbcTemplate.class, JdbcIndexedSessionRepository.class })
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(JdbcSessionProperties.class)
@Import({ DatabaseInitializationDependencyConfigurer.class, JdbcHttpSessionConfiguration.class })
public final class JdbcSessionAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(OnJdbcSessionDatasourceInitializationCondition.class)
	JdbcSessionDataSourceScriptDatabaseInitializer jdbcSessionDataSourceScriptDatabaseInitializer(
			@SpringSessionDataSource ObjectProvider<DataSource> sessionDataSource,
			ObjectProvider<DataSource> dataSource, JdbcSessionProperties properties) {
		DataSource dataSourceToInitialize = sessionDataSource.getIfAvailable(dataSource::getObject);
		return new JdbcSessionDataSourceScriptDatabaseInitializer(dataSourceToInitialize, properties);
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SessionRepositoryCustomizer<JdbcIndexedSessionRepository> springBootSessionRepositoryCustomizer(
			JdbcSessionProperties jdbcSessionProperties, SessionTimeout sessionTimeout) {
		return (sessionRepository) -> {
			PropertyMapper map = PropertyMapper.get();
			map.from(sessionTimeout::getTimeout).to(sessionRepository::setDefaultMaxInactiveInterval);
			map.from(jdbcSessionProperties::getTableName).to(sessionRepository::setTableName);
			map.from(jdbcSessionProperties::getFlushMode).to(sessionRepository::setFlushMode);
			map.from(jdbcSessionProperties::getSaveMode).to(sessionRepository::setSaveMode);
			map.from(jdbcSessionProperties::getCleanupCron).to(sessionRepository::setCleanupCron);
		};
	}

	static class OnJdbcSessionDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

		OnJdbcSessionDatasourceInitializationCondition() {
			super("Jdbc Session", "spring.session.jdbc.initialize-schema");
		}

	}

}
