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

import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriTemplateHandler
import java.util.*

/**
 * Standard implementation of [GitHub].
 * 
 * @author Andy Wilkinson
 */
internal class StandardGitHub(private val username: String?, private val password: String?) : GitHub {
    override fun getRepository(organization: String?, name: String?): GitHubRepository {
        val restTemplate = createRestTemplate()
        restTemplate.getInterceptors()
            .add(ClientHttpRequestInterceptor { request: HttpRequest?, body: ByteArray?, execution: ClientHttpRequestExecution? ->
                request!!.getHeaders().add("User-Agent", this@StandardGitHub.username)
                request.getHeaders()
                    .add(
                        "Authorization", "Basic " + Base64.getEncoder()
                            .encodeToString((this@StandardGitHub.username + ":" + this@StandardGitHub.password).toByteArray())
                    )
                request.getHeaders().add("Accept", MediaType.APPLICATION_JSON_VALUE)
                execution!!.execute(request, body!!)
            })
        val uriTemplateHandler: UriTemplateHandler = DefaultUriBuilderFactory(
            "https://api.github.com/repos/" + organization + "/" + name + "/"
        )
        restTemplate.setUriTemplateHandler(uriTemplateHandler)
        return StandardGitHubRepository(restTemplate)
    }

    @Suppress("deprecation")
    private fun createRestTemplate(): RestTemplate {
        return RestTemplate(mutableListOf<HttpMessageConverter<*>?>(JacksonJsonHttpMessageConverter()))
    }
}
