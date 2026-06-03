package io.github.kotlinmania.spring.boot.docs.io.grpc.testing.testtransport

interface HelloRequest {

	fun getName(): String

	companion object {

		fun newBuilder(): Builder {
			throw IllegalStateException()
		}

	}

	interface Builder {

		fun setName(name: String): Builder

		fun build(): HelloRequest

	}

}
