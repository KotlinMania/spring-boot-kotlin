import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::env[]
tasks.named<BootBuildImage>("bootBuildImage") {
	environment.putAll(mapOf("HTTP_PROXY" to "http://proxy.example.com",
						"HTTPS_PROXY" to "https://proxy.example.com"))
}
// end::env[]

tasks.register("bootBuildImageEnvironment") {
	doFirst {
		for((name, value) in tasks.getByName<BootBuildImage>("bootBuildImage").environment.get()) {
			print(name + "=" + value)
		}
	}
}
