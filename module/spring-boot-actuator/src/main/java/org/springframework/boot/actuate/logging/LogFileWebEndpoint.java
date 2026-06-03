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

package io.github.kotlinmania.spring.boot.actuate.logging;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.Endpoint;
import io.github.kotlinmania.spring.boot.actuate.endpoint.annotation.ReadOperation;
import io.github.kotlinmania.spring.boot.actuate.endpoint.web.annotation.WebEndpoint;
import io.github.kotlinmania.spring.boot.logging.LogFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Web {@link Endpoint @Endpoint} that provides access to an application's log file.
 *
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@WebEndpoint(id = "logfile")
public class LogFileWebEndpoint {

	private static final Log logger = LogFactory.getLog(LogFileWebEndpoint.class);

	private final @Nullable LogFile logFile;

	private final @Nullable File externalFile;

	public LogFileWebEndpoint(@Nullable LogFile logFile, @Nullable File externalFile) {
		this.logFile = logFile;
		this.externalFile = externalFile;
	}

	@ReadOperation(produces = "text/plain; charset=UTF-8")
	public @Nullable Resource logFile() {
		Resource logFileResource = getLogFileResource();
		if (logFileResource == null || !logFileResource.isReadable()) {
			return null;
		}
		return logFileResource;
	}

	private @Nullable Resource getLogFileResource() {
		if (this.externalFile != null) {
			return new FileSystemResource(this.externalFile);
		}
		if (this.logFile == null) {
			logger.debug("Missing 'logging.file.name' or 'logging.file.path' properties");
			return null;
		}
		return new FileSystemResource(this.logFile.toString());
	}

}
