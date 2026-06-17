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
package org.springframework.boot.build.bom.bomr.github

/**
 * Minimal API for interacting with a GitHub repository.
 * 
 * @author Andy Wilkinson
 */
interface GitHubRepository {
    /**
     * Opens a new issue with the given title. The given `labels` will be applied to
     * the issue and it will be assigned to the given `milestone`.
     * @param title the title of the issue
     * @param body the body of the issue
     * @param labels the labels to apply to the issue
     * @param milestone the milestone to assign the issue to
     * @return the number of the new issue
     */
    fun openIssue(title: String?, body: String?, labels: MutableList<String?>?, milestone: Milestone?): Int

    /**
     * Returns the labels in the repository.
     * @return the labels
     */
    val labels: MutableSet<String?>?

    /**
     * Returns the milestones in the repository.
     * @return the milestones
     */
    val milestones: MutableList<Milestone?>?

    /**
     * Finds issues that have the given `labels` and are assigned to the given
     * `milestone`.
     * @param labels issue labels
     * @param milestone assigned milestone
     * @return the matching issues
     */
    fun findIssues(labels: MutableList<String?>?, milestone: Milestone?): MutableList<Issue?>?
}
