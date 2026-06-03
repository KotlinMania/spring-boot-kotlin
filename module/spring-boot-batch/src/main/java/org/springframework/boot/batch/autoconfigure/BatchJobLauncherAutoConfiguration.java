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

package io.github.kotlinmania.spring.boot.batch.autoconfigure;

import org.springframework.batch.core.launch.JobOperator;
import io.github.kotlinmania.spring.boot.ExitCodeGenerator;
import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Batch. If a single job is
 * found in the context, it will be executed on startup.
 * <p>
 * Disable this behavior with {@literal spring.batch.job.enabled=false}).
 * <p>
 * If multiple jobs are found, a job name to execute on startup can be supplied by the
 * User with : {@literal spring.batch.job.name=job1}. In this case the Runner will first
 * find jobs registered as Beans, then those in the existing JobRegistry.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Kazuki Shimizu
 * @author Mahmoud Ben Hassine
 * @author Lars Uffmann
 * @author Lasse Wulff
 * @author Yanming Zhou
 * @since 4.0.0
 */
@AutoConfiguration(after = BatchAutoConfiguration.class)
@ConditionalOnClass(JobOperator.class)
@ConditionalOnBean(JobOperator.class)
@EnableConfigurationProperties(BatchProperties.class)
public final class BatchJobLauncherAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBooleanProperty(name = "spring.batch.job.enabled", matchIfMissing = true)
	JobLauncherApplicationRunner jobLauncherApplicationRunner(JobOperator jobOperator, BatchProperties properties) {
		JobLauncherApplicationRunner runner = new JobLauncherApplicationRunner(jobOperator);
		String jobName = properties.getJob().getName();
		if (StringUtils.hasText(jobName)) {
			runner.setJobName(jobName);
		}
		return runner;
	}

	@Bean
	@ConditionalOnMissingBean(ExitCodeGenerator.class)
	JobExecutionExitCodeGenerator jobExecutionExitCodeGenerator() {
		return new JobExecutionExitCodeGenerator();
	}

}
