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

package io.github.kotlinmania.spring.boot.devtools.autoconfigure;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import io.github.kotlinmania.spring.boot.autoconfigure.AutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.EnableAutoConfiguration;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import io.github.kotlinmania.spring.boot.autoconfigure.condition.ConditionalOnMissingBean;
import io.github.kotlinmania.spring.boot.context.properties.EnableConfigurationProperties;
import io.github.kotlinmania.spring.boot.devtools.autoconfigure.DevToolsProperties.Restart;
import io.github.kotlinmania.spring.boot.devtools.classpath.ClassPathChangedEvent;
import io.github.kotlinmania.spring.boot.devtools.classpath.ClassPathFileSystemWatcher;
import io.github.kotlinmania.spring.boot.devtools.classpath.ClassPathRestartStrategy;
import io.github.kotlinmania.spring.boot.devtools.classpath.PatternClassPathRestartStrategy;
import io.github.kotlinmania.spring.boot.devtools.filewatch.FileSystemWatcher;
import io.github.kotlinmania.spring.boot.devtools.filewatch.FileSystemWatcherFactory;
import io.github.kotlinmania.spring.boot.devtools.filewatch.SnapshotStateRepository;
import io.github.kotlinmania.spring.boot.devtools.restart.ConditionalOnInitializedRestarter;
import io.github.kotlinmania.spring.boot.devtools.restart.RestartScope;
import io.github.kotlinmania.spring.boot.devtools.restart.Restarter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for local development support.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vladimir Tsanev
 * @since 1.3.0
 */
@AutoConfiguration
@ConditionalOnInitializedRestarter
@EnableConfigurationProperties(DevToolsProperties.class)
public final class LocalDevToolsAutoConfiguration {

	/**
	 * Local LiveReload configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBooleanProperty(name = "spring.devtools.livereload.enabled")
	@SuppressWarnings("removal")
	static class LiveReloadConfiguration {

		@Bean
		@RestartScope
		@ConditionalOnMissingBean
		io.github.kotlinmania.spring.boot.devtools.livereload.LiveReloadServer liveReloadServer(DevToolsProperties properties) {
			return new io.github.kotlinmania.spring.boot.devtools.livereload.LiveReloadServer(
					properties.getLivereload().getPort(), Restarter.getInstance().getThreadFactory());
		}

		@Bean
		OptionalLiveReloadServer optionalLiveReloadServer(
				io.github.kotlinmania.spring.boot.devtools.livereload.LiveReloadServer liveReloadServer) {
			return new OptionalLiveReloadServer(liveReloadServer);
		}

		@Bean
		LiveReloadServerEventListener liveReloadServerEventListener(OptionalLiveReloadServer liveReloadServer) {
			return new LiveReloadServerEventListener(liveReloadServer);
		}

	}

	/**
	 * Local Restart Configuration.
	 */
	@Lazy(false)
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBooleanProperty(name = "spring.devtools.restart.enabled", matchIfMissing = true)
	static class RestartConfiguration {

		private final DevToolsProperties properties;

		RestartConfiguration(DevToolsProperties properties) {
			this.properties = properties;
		}

		@Bean
		RestartingClassPathChangeChangedEventListener restartingClassPathChangedEventListener(
				FileSystemWatcherFactory fileSystemWatcherFactory) {
			return new RestartingClassPathChangeChangedEventListener(fileSystemWatcherFactory);
		}

		@Bean
		@ConditionalOnMissingBean
		ClassPathFileSystemWatcher classPathFileSystemWatcher(FileSystemWatcherFactory fileSystemWatcherFactory,
				ClassPathRestartStrategy classPathRestartStrategy) {
			URL[] urls = Restarter.getInstance().getInitialUrls();
			Assert.state(urls != null, "'urls' must not be null");
			ClassPathFileSystemWatcher watcher = new ClassPathFileSystemWatcher(fileSystemWatcherFactory,
					classPathRestartStrategy, urls);
			watcher.setStopWatcherOnRestart(true);
			return watcher;
		}

		@Bean
		@ConditionalOnMissingBean
		ClassPathRestartStrategy classPathRestartStrategy() {
			return new PatternClassPathRestartStrategy(this.properties.getRestart().getAllExclude());
		}

		@Bean
		FileSystemWatcherFactory fileSystemWatcherFactory() {
			return this::newFileSystemWatcher;
		}

		@Bean
		@ConditionalOnBooleanProperty(name = "spring.devtools.restart.log-condition-evaluation-delta",
				matchIfMissing = true)
		ConditionEvaluationDeltaLoggingListener conditionEvaluationDeltaLoggingListener() {
			return new ConditionEvaluationDeltaLoggingListener();
		}

		private FileSystemWatcher newFileSystemWatcher() {
			Restart restartProperties = this.properties.getRestart();
			FileSystemWatcher watcher = new FileSystemWatcher(true, restartProperties.getPollInterval(),
					restartProperties.getQuietPeriod(), SnapshotStateRepository.STATIC);
			String triggerFile = restartProperties.getTriggerFile();
			if (StringUtils.hasLength(triggerFile)) {
				watcher.setTriggerFilter(new TriggerFileFilter(triggerFile));
			}
			List<File> additionalPaths = restartProperties.getAdditionalPaths();
			for (File path : additionalPaths) {
				watcher.addSourceDirectory(path.getAbsoluteFile());
			}
			return watcher;
		}

	}

	@SuppressWarnings("removal")
	static class LiveReloadServerEventListener implements GenericApplicationListener {

		private final OptionalLiveReloadServer liveReloadServer;

		LiveReloadServerEventListener(OptionalLiveReloadServer liveReloadServer) {
			this.liveReloadServer = liveReloadServer;
		}

		@Override
		public boolean supportsEventType(ResolvableType eventType) {
			Class<?> type = eventType.getRawClass();
			if (type == null) {
				return false;
			}
			return ContextRefreshedEvent.class.isAssignableFrom(type)
					|| ClassPathChangedEvent.class.isAssignableFrom(type);
		}

		@Override
		public boolean supportsSourceType(@Nullable Class<?> sourceType) {
			return true;
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ContextRefreshedEvent || (event instanceof ClassPathChangedEvent classPathChangedEvent
					&& !classPathChangedEvent.isRestartRequired())) {
				this.liveReloadServer.triggerReload();
			}
		}

		@Override
		public int getOrder() {
			return 0;
		}

	}

	static class RestartingClassPathChangeChangedEventListener implements ApplicationListener<ClassPathChangedEvent> {

		private static final Log logger = LogFactory.getLog(RestartingClassPathChangeChangedEventListener.class);

		private final FileSystemWatcherFactory fileSystemWatcherFactory;

		RestartingClassPathChangeChangedEventListener(FileSystemWatcherFactory fileSystemWatcherFactory) {
			this.fileSystemWatcherFactory = fileSystemWatcherFactory;
		}

		@Override
		public void onApplicationEvent(ClassPathChangedEvent event) {
			if (event.isRestartRequired()) {
				logger.info(LogMessage.format("Restarting due to %s", event.overview()));
				logger.debug(LogMessage.format("Change set: %s", event.getChangeSet()));
				Restarter.getInstance().restart(new FileWatchingFailureHandler(this.fileSystemWatcherFactory));
			}
		}

	}

}
