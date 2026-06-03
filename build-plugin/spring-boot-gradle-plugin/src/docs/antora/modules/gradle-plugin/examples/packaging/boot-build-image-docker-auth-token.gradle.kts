import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootJar
import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::docker-auth-token[]
tasks.named<BootBuildImage>("bootBuildImage") {
	docker {
		builderRegistry {
			token.set("9cbaf023786cd7...")
		}
	}
}
// end::docker-auth-token[]

tasks.register("bootBuildImageDocker") {
	doFirst {
		println("token=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.builderRegistry.token.get()}")
	}
}
