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

package io.github.kotlinmania.spring.boot.actuate.autoconfigure.info;

import io.github.kotlinmania.spring.boot.actuate.info.BuildInfoContributor;
import io.github.kotlinmania.spring.boot.actuate.info.EnvironmentInfoContributor;
import io.github.kotlinmania.spring.boot.actuate.info.GitInfoContributor;
import io.github.kotlinmania.spring.boot.actuate.info.InfoContributor;
import io.github.kotlinmania.spring.boot.actuate.info.JavaInfoContributor;
import io.github.kotlinmania.spring.boot.actuate.info.OsInfoContributor;
import io.github.kotlinmania.spring.boot.actuate.info.ProcessInfoContributor;
import io.github.kotlinmania.spring.boot.actuate.info.SslInfoContributor;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import io.github.kotlinmania.spring.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.info.BuildProperties;
import io.github.kotlinmania.spring.boot.info.GitProperties;
import io.github.kotlinmania.spring.boot.info.SslInfo;
import io.github.kotlinmania.spring.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for standard
 * {@link InfoContributor}s.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @author Jonatan Ivanov
 * @since 2.0.0
 */
@AutoConfiguration(after = ProjectInfoAutoConfiguration.class)
@EnableConfigurationProperties(InfoContributorProperties.class)
public final class InfoContributorAutoConfiguration {

	/**
	 * The default order for the core {@link InfoContributor} beans.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	@Bean
	@ConditionalOnEnabledInfoContributor(value = "env", fallback = InfoContributorFallback.DISABLE)
	@Order(DEFAULT_ORDER)
	EnvironmentInfoContributor envInfoContributor(ConfigurableEnvironment environment) {
		return new EnvironmentInfoContributor(environment);
	}

	@Bean
	@ConditionalOnEnabledInfoContributor("git")
	@ConditionalOnSingleCandidate(GitProperties.class)
	@ConditionalOnMissingBean
	@Order(DEFAULT_ORDER)
	GitInfoContributor gitInfoContributor(GitProperties gitProperties,
			InfoContributorProperties infoContributorProperties) {
		return new GitInfoContributor(gitProperties, infoContributorProperties.getGit().getMode());
	}

	@Bean
	@ConditionalOnEnabledInfoContributor("build")
	@ConditionalOnSingleCandidate(BuildProperties.class)
	@Order(DEFAULT_ORDER)
	InfoContributor buildInfoContributor(BuildProperties buildProperties) {
		return new BuildInfoContributor(buildProperties);
	}

	@Bean
	@ConditionalOnEnabledInfoContributor(value = "java", fallback = InfoContributorFallback.DISABLE)
	@Order(DEFAULT_ORDER)
	JavaInfoContributor javaInfoContributor() {
		return new JavaInfoContributor();
	}

	@Bean
	@ConditionalOnEnabledInfoContributor(value = "os", fallback = InfoContributorFallback.DISABLE)
	@Order(DEFAULT_ORDER)
	OsInfoContributor osInfoContributor() {
		return new OsInfoContributor();
	}

	@Bean
	@ConditionalOnEnabledInfoContributor(value = "process", fallback = InfoContributorFallback.DISABLE)
	@Order(DEFAULT_ORDER)
	ProcessInfoContributor processInfoContributor() {
		return new ProcessInfoContributor();
	}

	@Bean
	@ConditionalOnEnabledInfoContributor(value = "ssl", fallback = InfoContributorFallback.DISABLE)
	@Order(DEFAULT_ORDER)
	SslInfoContributor sslInfoContributor(SslInfo sslInfo) {
		return new SslInfoContributor(sslInfo);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledInfoContributor(value = "ssl", fallback = InfoContributorFallback.DISABLE)
	SslInfo sslInfo(SslBundles sslBundles) {
		return new SslInfo(sslBundles);
	}

}
