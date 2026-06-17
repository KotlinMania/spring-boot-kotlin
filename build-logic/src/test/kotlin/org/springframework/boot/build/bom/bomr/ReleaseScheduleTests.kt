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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.build.bom.bomr.ReleaseSchedule.Release
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.time.OffsetDateTime

/**
 * Tests for [ReleaseSchedule].
 * 
 * @author Andy Wilkinson
 */
internal class ReleaseScheduleTests {
    private val rest: RestTemplate = RestTemplate()

    private val releaseSchedule: ReleaseSchedule = ReleaseSchedule(this.rest)

    private val server: MockRestServiceServer = MockRestServiceServer.bindTo(this.rest).build()

    @Test
    fun releasesBetween() {
        this.server
            .expect(requestTo("https://calendar.spring.io/releases?start=2023-09-01T00:00Z&end=2023-09-21T23:59Z"))
            .andRespond(withSuccess(ClassPathResource("releases.json"), MediaType.APPLICATION_JSON))
        val releases: Map<String?, List<Release?>?> = this.releaseSchedule
            .releasesBetween(OffsetDateTime.parse("2023-09-01T00:00Z"), OffsetDateTime.parse("2023-09-21T23:59Z"))
        assertThat(releases).hasSize(23)
        assertThat(releases.get("Spring Framework")).hasSize(3)
        assertThat(releases.get("Spring Boot")).hasSize(4)
        assertThat(releases.get("Spring Modulith")).hasSize(1)
        assertThat(releases.get("spring graphql")).hasSize(3)
    }
}
