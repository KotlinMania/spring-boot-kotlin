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
package org.springframework.boot.build.bom.bomr

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.build.bom.Library
import java.util.concurrent.*
import java.util.stream.Stream

/**
 * [LibraryUpdateResolver] decorator that uses multiple threads to find library
 * updates.
 * 
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
internal class MultithreadedLibraryUpdateResolver(
    private val threads: Int,
    private val delegate: LibraryUpdateResolver
) : LibraryUpdateResolver {
    override fun findLibraryUpdates(
        librariesToUpgrade: MutableCollection<Library?>,
        librariesByName: MutableMap<String?, Library?>?
    ): MutableList<LibraryWithVersionOptions?> {
        logger.info("Looking for updates using {} threads", this.threads)
        val executorService = Executors.newFixedThreadPool(this.threads)
        try {
            return librariesToUpgrade.stream()
                .map<Future<MutableList<LibraryWithVersionOptions?>?>?> { library: Library? ->
                    if (library!!.versionAlignment == null) {
                        return@map executorService.submit<MutableList<LibraryWithVersionOptions?>?>(Callable {
                            this.delegate
                                .findLibraryUpdates(mutableListOf<Library?>(library), librariesByName)
                        })
                    } else {
                        return@map CompletableFuture.completedFuture<MutableList<LibraryWithVersionOptions?>?>(
                            this.delegate.findLibraryUpdates(mutableListOf<Library?>(library), librariesByName)
                        )
                    }
                }.flatMap<LibraryWithVersionOptions?> { job: Future<MutableList<LibraryWithVersionOptions?>?>? ->
                    this.getResult(job!!)
                }.toList()
        } finally {
            executorService.shutdownNow()
        }
    }

    private fun getResult(job: Future<MutableList<LibraryWithVersionOptions?>?>): Stream<LibraryWithVersionOptions?> {
        try {
            return job.get()!!.stream()
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException(ex)
        } catch (ex: ExecutionException) {
            throw RuntimeException(ex)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MultithreadedLibraryUpdateResolver::class.java)
    }
}
