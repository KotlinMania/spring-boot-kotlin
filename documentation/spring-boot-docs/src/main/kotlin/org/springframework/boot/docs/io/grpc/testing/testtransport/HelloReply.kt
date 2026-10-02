package io.github.kotlinmania.spring.boot.docs.io.grpc.testing.testtransport

interface HelloReply {

	fun getMessage(): String

	companion object {

		fun newBuilder(): Builder {
			throw IllegalStateException()
		}

	}

	interface Builder {

		fun setMessage(message: String): Builder

		fun build(): HelloReply

	}

}
