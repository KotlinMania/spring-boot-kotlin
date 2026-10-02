package io.github.kotlinmania.spring.boot.docs.io.grpc.client.stubbeans

class HelloWorldGrpc {

	interface HelloWorldBlockingStub {

		fun sayHello(request: HelloRequest): HelloReply

	}

}
