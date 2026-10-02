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

package io.github.kotlinmania.spring.boot.mustache.autoconfigure;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.TemplateLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnClass;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.autoconfigure.template.TemplateLocation;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mustache.
 *
 * @author Dave Syer
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Mustache.class)
@EnableConfigurationProperties(MustacheProperties.class)
@Import({ MustacheServletWebConfiguration.class, MustacheReactiveWebConfiguration.class })
public final class MustacheAutoConfiguration {

	private static final Log logger = LogFactory.getLog(MustacheAutoConfiguration.class);

	private final MustacheProperties mustache;

	private final ApplicationContext applicationContext;

	MustacheAutoConfiguration(MustacheProperties mustache, ApplicationContext applicationContext) {
		this.mustache = mustache;
		this.applicationContext = applicationContext;
		checkTemplateLocationExists();
	}

	private void checkTemplateLocationExists() {
		if (this.mustache.isCheckTemplateLocation()) {
			TemplateLocation location = new TemplateLocation(this.mustache.getPrefix());
			if (!location.exists(this.applicationContext) && logger.isWarnEnabled()) {
				logger.warn("Cannot find template location: " + location
						+ " (please add some templates, check your Mustache configuration, or set spring.mustache."
						+ "check-template-location=false)");
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean
	Mustache.Compiler mustacheCompiler(TemplateLoader mustacheTemplateLoader) {
		return Mustache.compiler().withLoader(mustacheTemplateLoader);
	}

	@Bean
	@ConditionalOnMissingBean(TemplateLoader.class)
	MustacheResourceTemplateLoader mustacheTemplateLoader() {
		MustacheResourceTemplateLoader loader = new MustacheResourceTemplateLoader(this.mustache.getPrefix(),
				this.mustache.getSuffix());
		loader.setCharset(this.mustache.getCharset());
		return loader;
	}

}
