package io.github.kotlinmania.spring.boot.docs.io.grpc.client.stubbeans

import io.github.kotlinmania.spring.boot.ApplicationArguments
import io.github.kotlinmania.spring.boot.ApplicationRunner


class MyApplicationRunner(val helloStub: HelloWorldGrpc.HelloWorldBlockingStub) : ApplicationRunner {

	override fun run(args: ApplicationArguments) {
		val request = HelloRequest.newBuilder().setName("Spring").build()
		val reply: HelloReply = helloStub.sayHello(request)
		println(reply.getMessage())
	}

}
