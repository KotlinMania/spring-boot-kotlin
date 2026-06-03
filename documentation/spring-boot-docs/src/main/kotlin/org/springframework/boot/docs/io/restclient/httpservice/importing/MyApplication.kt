package io.github.kotlinmania.spring.boot.docs.io.restclient.httpservice.importing

import io.github.kotlinmania.spring.boot.autoconfigure.SpringBootApplication
import io.github.kotlinmania.spring.boot.runApplication
import org.springframework.web.service.registry.ImportHttpServices

@SpringBootApplication
@ImportHttpServices(basePackages = ["com.example.myclients"])
class MyApplication

fun main(args: Array<String>) {
	runApplication<MyApplication>(*args)
}

