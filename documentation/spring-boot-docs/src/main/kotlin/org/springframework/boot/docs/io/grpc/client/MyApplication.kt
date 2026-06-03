package io.github.kotlinmania.spring.boot.docs.io.grpc.client

import io.github.kotlinmania.spring.boot.autoconfigure.SpringBootApplication
import io.github.kotlinmania.spring.boot.docs.features.springapplication.MyApplication
import io.github.kotlinmania.spring.boot.runApplication
import org.springframework.grpc.client.ImportGrpcClients

@SpringBootApplication(proxyBeanMethods = false)
@ImportGrpcClients(target = "hello", types = [HelloWorldGrpc.HelloWorldBlockingStub::class])
class MyApplication

fun main(args: Array<String>) {
	runApplication<MyApplication>(*args)
}
