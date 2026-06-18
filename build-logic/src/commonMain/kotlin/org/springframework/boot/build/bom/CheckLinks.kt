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
package org.springframework.boot.build.bom

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.apache.http.client.config.CookieSpecs
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.NoOpResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.net.URISyntaxException
import java.util.function.Consumer
import javax.inject.Inject

/**
 * Task to check that links are working.
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract class CheckLinks @Inject constructor(private val bom: BomExtension) : DefaultTask() {
    @TaskAction
    fun releaseNotes() {
        val config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build()
        val httpClient = HttpClients.custom().setDefaultRequestConfig(config).build()
        val requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)
        val restTemplate = RestTemplate(requestFactory)
        restTemplate.setErrorHandler(NoOpResponseErrorHandler())
        for (library in this.bom.libraries) {
            library.getLinks().forEach { (name: String?, links: MutableList<Library.Link?>?) ->
                links!!.forEach(Consumer { link: Library.Link? ->
                    val uri: URI?
                    try {
                        uri = URI(link!!.url(library))
                        val response: ResponseEntity<String?> =
                            restTemplate.exchange<String?>(uri, HttpMethod.HEAD, null, String::class.java)
                        System.out.printf(
                            "[%3d] %s - %s (%s)%n", response.getStatusCode().value(), library.name, name,
                            uri
                        )
                    } catch (ex: URISyntaxException) {
                        throw RuntimeException(ex)
                    }
                })
            }
        }
    }
}
