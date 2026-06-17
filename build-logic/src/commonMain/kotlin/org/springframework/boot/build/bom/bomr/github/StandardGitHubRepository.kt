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

import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.lang.String
import java.time.Duration
import java.time.OffsetDateTime
import java.util.function.Function
import kotlin.Any
import kotlin.Int
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.get

/**
 * Standard implementation of [GitHubRepository].
 * 
 * @author Andy Wilkinson
 */
internal class StandardGitHubRepository(private val rest: RestTemplate) : GitHubRepository {
    override fun openIssue(title: String?, body: String?, labels: MutableList<String?>, milestone: Milestone?): Int {
        val requestBody: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        requestBody.put("title", title)
        if (milestone != null) {
            requestBody.put("milestone", milestone.getNumber())
        }
        if (!labels.isEmpty()) {
            requestBody.put("labels", labels)
        }
        requestBody.put("body", body)
        try {
            val response: ResponseEntity<MutableMap<*, *>?> =
                this.rest.postForEntity<MutableMap<*, *>?>("issues", requestBody, MutableMap::class.java)
            // See gh-30304
            sleep(Duration.ofSeconds(3))
            return (response.getBody().get("number") as kotlin.Int?)!!
        } catch (ex: RestClientException) {
            if (ex is HttpClientErrorException.Forbidden) {
                println("Received 403 response with headers " + ex.getResponseHeaders())
            }
            throw ex
        }
    }

    override fun getLabels(): MutableSet<String?> {
        return HashSet<String?>(
            get<String?>(
                "labels?per_page=100",
                Function { label: MutableMap<String?, Any?>? -> label!!.get("name") as String? })
        )
    }

    override fun getMilestones(): MutableList<Milestone?> {
        return get<Milestone?>("milestones?per_page=100", Function { milestone: MutableMap<String?, Any?>? ->
            Milestone(
                milestone!!.get("title") as String?,
                (milestone.get("number") as kotlin.Int?)!!,
                if (milestone.get("due_on") != null) OffsetDateTime.parse(milestone.get("due_on") as String?) else null
            )
        })
    }

    override fun findIssues(labels: MutableList<String?>, milestone: Milestone): MutableList<Issue?> {
        return get<Issue?>(
            ("issues?per_page=100&state=all&labels=" + String.join(",", labels) + "&milestone="
                    + milestone.getNumber()),
            Function { issue: MutableMap<kotlin.String?, Any?>? ->
                Issue(
                    this.rest, (issue!!.get("number") as kotlin.Int?)!!, issue.get("title") as kotlin.String?,
                    Issue.State.Companion.of(issue.get("state") as kotlin.String?)
                )
            })
    }

    private fun <T> get(
        name: kotlin.String,
        mapper: Function<MutableMap<kotlin.String?, Any?>?, T?>?
    ): MutableList<T?> {
        val response: ResponseEntity<MutableList<*>?> =
            this.rest.getForEntity<MutableList<*>?>(name, MutableList::class.java)
        return (response.getBody() as MutableList<MutableMap<kotlin.String?, Any?>?>).stream().map<T?>(mapper).toList()
    }

    companion object {
        private fun sleep(duration: Duration) {
            try {
                Thread.sleep(duration.toMillis())
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
