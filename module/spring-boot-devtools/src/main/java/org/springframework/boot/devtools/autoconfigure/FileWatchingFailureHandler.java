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

import java.util.Set;
import java.util.concurrent.CountDownLatch;

import io.github.kotlinmania.spring.boot.devtools.classpath.ClassPathDirectories;
import io.github.kotlinmania.spring.boot.devtools.filewatch.ChangedFiles;
import io.github.kotlinmania.spring.boot.devtools.filewatch.FileChangeListener;
import io.github.kotlinmania.spring.boot.devtools.filewatch.FileSystemWatcher;
import io.github.kotlinmania.spring.boot.devtools.filewatch.FileSystemWatcherFactory;
import io.github.kotlinmania.spring.boot.devtools.restart.FailureHandler;
import io.github.kotlinmania.spring.boot.devtools.restart.Restarter;

/**
 * {@link FailureHandler} that waits for filesystem changes before retrying.
 *
 * @author Phillip Webb
 */
class FileWatchingFailureHandler implements FailureHandler {

	private final FileSystemWatcherFactory fileSystemWatcherFactory;

	FileWatchingFailureHandler(FileSystemWatcherFactory fileSystemWatcherFactory) {
		this.fileSystemWatcherFactory = fileSystemWatcherFactory;
	}

	@Override
	public Outcome handle(Throwable failure) {
		CountDownLatch latch = new CountDownLatch(1);
		FileSystemWatcher watcher = this.fileSystemWatcherFactory.getFileSystemWatcher();
		watcher.addSourceDirectories(new ClassPathDirectories(Restarter.getInstance().getInitialUrls()));
		watcher.addListener(new Listener(latch));
		watcher.start();
		try {
			latch.await();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return Outcome.RETRY;
	}

	private static class Listener implements FileChangeListener {

		private final CountDownLatch latch;

		Listener(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void onChange(Set<ChangedFiles> changeSet) {
			this.latch.countDown();
		}

	}

}
