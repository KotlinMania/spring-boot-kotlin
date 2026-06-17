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

import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedCaseInsensitiveMap
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Release schedule for Spring projects, retrieved from
 * [https://calendar.spring.io](https://calendar.spring.io).
 * 
 * @author Andy Wilkinson
 */
class ReleaseSchedule @JvmOverloads constructor(private val rest: RestOperations = RestTemplate()) {
    fun releasesBetween(start: OffsetDateTime?, end: OffsetDateTime?): MutableMap<String?, MutableList<Release?>?> {
        val response: ResponseEntity<MutableList<*>?> = this.rest
            .getForEntity<MutableList<*>?>(
                "https://calendar.spring.io/releases?start=" + start + "&end=" + end,
                MutableList::class.java
            )
        val body: MutableList<MutableMap<String?, String?>?>? = response.body
        val releasesByLibrary: MutableMap<String?, MutableList<Release?>?> =
            LinkedCaseInsensitiveMap<MutableList<Release?>?>()
        body!!.stream()
            .map<Release> { entry: MutableMap<String?, String?>? -> this.asRelease(entry) }
            .filter { obj: Release? -> Objects.nonNull(obj) }
            .forEach { release: Release? ->
                releasesByLibrary.computeIfAbsent(
                    release!!.libraryName
                ) { l: kotlin.String? -> java.util.ArrayList<org.springframework.boot.build.bom.bomr.ReleaseSchedule.Release?>() }!!
                    .add(release)
            }
        return releasesByLibrary
    }

    private fun asRelease(entry: MutableMap<String?, String>): Release? {
        val due = LocalDate.parse(entry.get("start"))
        val title: String = entry.get("title")!!
        val matcher: Matcher = LIBRARY_AND_VERSION.matcher(title)
        if (!matcher.matches()) {
            return null
        }
        val library = matcher.group(1)
        val version = matcher.group(2)
        return Release(library, DependencyVersion.parse(version), due)
    }

    class Release(val libraryName: String?, val version: DependencyVersion?, val dueOn: LocalDate?)

    companion object {
        private val LIBRARY_AND_VERSION: Pattern = Pattern.compile("([A-Za-z0-9 ]+) ([0-9A-Za-z.-]+)")
    }
}
