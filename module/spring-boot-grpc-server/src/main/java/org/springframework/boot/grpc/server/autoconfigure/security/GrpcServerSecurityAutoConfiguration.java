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

package io.github.kotlinmania.spring.boot.grpc.server.autoconfigure.security;

import io.grpc.BindableService;
import io.grpc.internal.GrpcUtil;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBean;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.grpc.server.GrpcServletRegistration;
import io.github.kotlinmania.spring.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import io.github.kotlinmania.spring.boot.grpc.server.autoconfigure.GrpcServerExecutorProvider;
import io.github.kotlinmania.spring.boot.grpc.server.autoconfigure.security.GrpcServerSecurityAutoConfiguration.ExceptionHandlerConfiguration;
import io.github.kotlinmania.spring.boot.grpc.server.autoconfigure.security.GrpcServerSecurityAutoConfiguration.GrpcNativeSecurityConfigurerConfiguration;
import io.github.kotlinmania.spring.boot.grpc.server.autoconfigure.security.GrpcServerSecurityAutoConfiguration.GrpcServletSecurityConfigurerConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.security.CoroutineSecurityContextInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.SecurityContextServerInterceptor;
import org.springframework.grpc.server.security.SecurityGrpcExceptionHandler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side security.
 *
 * @author Dave Syer
 * @author Chris Bono
 * @author Andrey Litvitski
 * @author Phillip Webb
 * @since 4.1.0
 */
@AutoConfiguration(after = GrpcServerAutoConfiguration.class,
		afterName = "io.github.kotlinmania.spring.boot.security.autoconfigure.SecurityAutoConfiguration")
@ConditionalOnBooleanProperty(name = "spring.grpc.server.enabled", matchIfMissing = true)
@ConditionalOnClass({ BindableService.class, GrpcServerFactory.class, ObjectPostProcessor.class })
@Import({ ExceptionHandlerConfiguration.class, GrpcNativeSecurityConfigurerConfiguration.class,
		GrpcServletSecurityConfigurerConfiguration.class })
public final class GrpcServerSecurityAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@Import(AuthenticationConfiguration.class)
	static class ExceptionHandlerConfiguration {

		@Bean
		SecurityGrpcExceptionHandler accessExceptionHandler() {
			return new SecurityGrpcExceptionHandler();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(GrpcServerFactory.class)
	@EnableGlobalAuthentication
	static class GrpcNativeSecurityConfigurerConfiguration {

		@Bean
		GrpcSecurity grpcSecurity(ApplicationContext context, ObjectPostProcessor<Object> objectPostProcessor,
				AuthenticationConfiguration authenticationConfiguration) {
			AuthenticationManagerBuilder authenticationManagerBuilder = authenticationConfiguration
				.authenticationManagerBuilder(objectPostProcessor, context);
			authenticationManagerBuilder
				.parentAuthenticationManager(authenticationConfiguration.getAuthenticationManager());
			return new GrpcSecurity(objectPostProcessor, authenticationManagerBuilder, context);
		}

	}

	@ConditionalOnBean({ GrpcServletRegistration.class, SecurityFilterChain.class })
	@Configuration(proxyBeanMethods = false)
	static class GrpcServletSecurityConfigurerConfiguration {

		@Bean
		@GlobalServerInterceptor
		SecurityContextServerInterceptor securityContextInterceptor() {
			return new SecurityContextServerInterceptor();
		}

		@Bean
		@ConditionalOnMissingBean
		GrpcServerExecutorProvider grpcServerExecutorProvider() {
			return () -> new DelegatingSecurityContextExecutor(GrpcUtil.SHARED_CHANNEL_EXECUTOR.create());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "io.grpc.kotlin.CoroutineContextServerInterceptor")
	static class GrpcServerCoroutineStubConfiguration {

		@Bean
		@GlobalServerInterceptor
		@ConditionalOnMissingBean
		CoroutineSecurityContextInterceptor coroutineSecurityContextInterceptor() {
			return new CoroutineSecurityContextInterceptor();
		}

	}

}
