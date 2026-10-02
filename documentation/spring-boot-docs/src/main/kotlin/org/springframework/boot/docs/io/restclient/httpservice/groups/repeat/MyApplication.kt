package io.github.kotlinmania.spring.boot.docs.io.restclient.httpservice.groups.repeat

import io.github.kotlinmania.spring.boot.autoconfigure.SpringBootApplication
import io.github.kotlinmania.spring.boot.runApplication
import org.springframework.web.service.registry.ImportHttpServices

@SpringBootApplication
@ImportHttpServices(group = "echo", types = [EchoService::class])
@ImportHttpServices(group = "other", types = [OtherService::class])
class MyApplication

fun main(args: Array<String>) {
	runApplication<MyApplication>(*args)
}

