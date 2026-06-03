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

package io.github.kotlinmania.spring.boot.data.jpa.autoconfigure;

import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.Test;

import io.github.kotlinmania.spring.boot.LazyInitializationBeanFactoryPostProcessor;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfigurations;
import io.github.kotlinmania.spring.boot.autoconfigure.TestAutoConfigurationPackage;
import io.github.kotlinmania.spring.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import io.github.kotlinmania.spring.boot.data.jpa.autoconfigure.domain.city.City;
import io.github.kotlinmania.spring.boot.data.jpa.autoconfigure.domain.city.CityRepository;
import io.github.kotlinmania.spring.boot.data.jpa.autoconfigure.domain.country.Country;
import io.github.kotlinmania.spring.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import io.github.kotlinmania.spring.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import io.github.kotlinmania.spring.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for {@link DataJpaRepositoriesAutoConfiguration} tests.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Scott Frederick
 * @author Stefano Cordio
 */
abstract class AbstractDataJpaRepositoriesAutoConfigurationTests {

	final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HibernateJpaAutoConfiguration.class,
				DataJpaRepositoriesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class))
		.withUserConfiguration(EmbeddedDataSourceConfiguration.class);

	@Test
	void testDefaultRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CityRepository.class);
			assertThat(context).hasSingleBean(PlatformTransactionManager.class);
			assertThat(context).hasSingleBean(EntityManagerFactory.class);
			assertThat(context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor()).isNull();
		});
	}

	@Test
	void testOverrideRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CityRepository.class);
			assertThat(context).hasSingleBean(PlatformTransactionManager.class);
			assertThat(context).hasSingleBean(EntityManagerFactory.class);
		});
	}

	@Test
	void autoConfigurationShouldNotKickInEvenIfManualConfigDidNotCreateAnyRepositories() {
		this.contextRunner.withUserConfiguration(SortOfInvalidCustomConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(CityRepository.class));
	}

	@Test
	void whenBootstrapModeDoesNotUseFallbackBootstrapExecutor() {
		this.contextRunner.withUserConfiguration(SingleAsyncTaskExecutorConfiguration.class)
			.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=lazy")
			.run((context) -> assertThat(
					context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
				.isNull());
	}

	@Test
	void whenBootstrapModeIsDeferredBootstrapExecutorIsConfigured() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class))
			.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=deferred")
			.run((context) -> assertThat(
					context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
				.isEqualTo(context.getBean("applicationTaskExecutor")));
	}

	@Test
	void whenBootstrapModeIsDefaultBootstrapExecutorIsNotConfigured() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class))
			.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=default")
			.run((context) -> assertThat(
					context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
				.isNull());
	}

	@Test
	void bootstrapModeIsDefaultByDefault() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class))
			.run((context) -> assertThat(
					context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
				.isNull());
	}

	@Test
	void whenLazyInitializationIsEnabledJpaMetamodelCacheIsClearedOnContextClose() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.withBean(LazyInitializationBeanFactoryPostProcessor.class)
			.run((context) -> assertThat(jpaMetamodelCache()).isNotEmpty());
		assertThat(jpaMetamodelCache()).isEmpty();
	}

	@SuppressWarnings("unchecked")
	private Map<Metamodel, JpaMetamodel> jpaMetamodelCache() {
		Object field = ReflectionTestUtils.getField(JpaMetamodel.class, "CACHE");
		assertThat(field).isNotNull();
		return (Map<Metamodel, JpaMetamodel>) field;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@Import(TestConfiguration.class)
	static class MultipleAsyncTaskExecutorConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestConfiguration.class)
	static class SingleAsyncTaskExecutorConfiguration {

		@Bean
		SimpleAsyncTaskExecutor testAsyncTaskExecutor() {
			return new SimpleAsyncTaskExecutor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableJpaRepositories(basePackageClasses = CityRepository.class)
	@TestAutoConfigurationPackage(City.class)
	static class CustomConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	// To not find any repositories
	@EnableJpaRepositories("foo.bar")
	@TestAutoConfigurationPackage(City.class)
	static class SortOfInvalidCustomConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(Country.class)
	static class RevisionRepositoryConfiguration {

	}

}
