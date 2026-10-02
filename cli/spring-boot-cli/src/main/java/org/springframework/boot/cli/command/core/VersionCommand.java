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

package io.github.kotlinmania.spring.boot.cli.command.core;

import io.github.kotlinmania.spring.boot.cli.command.AbstractCommand;
import io.github.kotlinmania.spring.boot.cli.command.Command;
import io.github.kotlinmania.spring.boot.cli.command.status.ExitStatus;
import io.github.kotlinmania.spring.boot.cli.util.Log;

/**
 * {@link Command} to display the 'version' number.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class VersionCommand extends AbstractCommand {

	public VersionCommand() {
		super("version", "Show the version");
	}

	@Override
	public ExitStatus run(String... args) {
		Log.info("Spring CLI v" + getClass().getPackage().getImplementationVersion());
		return ExitStatus.OK;
	}

}
