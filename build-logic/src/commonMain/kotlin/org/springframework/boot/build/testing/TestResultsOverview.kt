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
package org.springframework.boot.build.testing

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.lang.AutoCloseable
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

/**
 * [BuildService] that provides an overview of all the test failures in the build.
 * 
 * @author Andy Wilkinson
 */
abstract class TestResultsOverview

    : BuildService<BuildServiceParameters.None?>, OperationCompletionListener, AutoCloseable {
    private val testFailures: MutableMap<Test?, MutableList<TestFailure?>?> =
        TreeMap<Test?, MutableList<TestFailure?>?>(
            Comparator.comparing<Test?, String?>(Function { obj: Test? -> obj!!.getPath() })
        )

    private val monitor = Any()

    fun addFailures(test: Test?, failureDescriptors: MutableList<TestDescriptor?>) {
        val testFailures =
            failureDescriptors.stream().map<TestFailure?> { descriptor: TestDescriptor? -> TestFailure(descriptor!!) }
                .sorted().toList()
        synchronized(this.monitor) {
            this.testFailures.put(test, testFailures)
        }
    }

    override fun onFinish(event: FinishEvent?) {
        // OperationCompletionListener is implemented to defer close until the build ends
    }

    override fun close() {
        synchronized(this.monitor) {
            if (this.testFailures.isEmpty()) {
                return
            }
            System.err.println()
            System.err.println(
                ("Found test failures in " + this.testFailures.size + " test task"
                        + (if (this.testFailures.size == 1) ":" else "s:"))
            )
            this.testFailures.forEach { (task: Test?, failures: MutableList<TestFailure?>?) ->
                System.err.println()
                System.err.println(task!!.getPath())
                failures!!.forEach(Consumer { failure: TestFailure? ->
                    System.err
                        .println("    " + failure.descriptor.getClassName() + " > " + failure.descriptor.getName())
                })
            }
        }
    }

    private class TestFailure(private val descriptor: TestDescriptor) : Comparable<TestFailure?> {
        override fun compareTo(other: TestFailure): Int {
            var comparison = this.descriptor.getClassName()!!.compareTo(other.descriptor.getClassName()!!)
            if (comparison == 0) {
                comparison = this.descriptor.getName().compareTo(other.descriptor.getName())
            }
            return comparison
        }
    }
}
