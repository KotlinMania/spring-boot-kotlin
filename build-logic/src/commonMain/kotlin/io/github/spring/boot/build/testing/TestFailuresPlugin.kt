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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

/**
 * Plugin for recording test failures and reporting them at the end of the build.
 * 
 * @author Andy Wilkinson
 */
class TestFailuresPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val testResultsOverview = project.getGradle()
            .getSharedServices()
            .registerIfAbsent<TestResultsOverview?, BuildServiceParameters.None?>(
                "testResultsOverview",
                TestResultsOverview::class.java,
                Action { spec: BuildServiceSpec<BuildServiceParameters.None?> -> })
        project.getTasks().withType<Test>(Test::class.java, Action { test: Test ->
            test!!.usesService(testResultsOverview)
            test.addTestListener(FailureRecordingTestListener(testResultsOverview, test))
        })
    }

    private inner class FailureRecordingTestListener(
        private val testResultsOverview: Provider<TestResultsOverview>,
        private val test: Test?
    ) : TestListener {
        private val failures: MutableList<TestDescriptor?> = ArrayList<TestDescriptor?>()

        override fun afterSuite(descriptor: TestDescriptor?, result: TestResult?) {
            if (!this.failures.isEmpty()) {
                this.testResultsOverview.get()!!.addFailures(this.test, this.failures)
            }
        }

        override fun afterTest(descriptor: TestDescriptor?, result: TestResult) {
            if (result.getFailedTestCount() > 0) {
                this.failures.add(descriptor)
            }
        }

        override fun beforeSuite(descriptor: TestDescriptor?) {
        }

        override fun beforeTest(descriptor: TestDescriptor?) {
        }
    }
}
