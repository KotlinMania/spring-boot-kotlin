package io.github.kotlinmania.spring.boot.resttestclient

import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import kotlin.test.assertEquals

class TestRestTemplateExtensionsTest {
    @Test
    fun getForObjectUsesReifiedResponseType() {
        val client = TestRestTemplate()
        val server = MockRestServiceServer.bindTo(client.restTemplate).build()
        server
            .expect(requestTo("https://example.test/message"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("hello", MediaType.TEXT_PLAIN))

        assertEquals("hello", client.getForObject<String>("https://example.test/message"))
        server.verify()
    }

    @Test
    fun exchangeRetainsGenericResponseType() {
        val client = TestRestTemplate()
        val server = MockRestServiceServer.bindTo(client.restTemplate).build()
        server
            .expect(requestTo("https://example.test/messages"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("[\"one\",\"two\"]", MediaType.APPLICATION_JSON))

        val response = client.exchange<List<String>>("https://example.test/messages", HttpMethod.GET)
        assertEquals(listOf("one", "two"), response.body)
        server.verify()
    }
}
