import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootJar
import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::docker-host[]
tasks.named<BootBuildImage>("bootBuildImage") {
	docker {
		host.set("unix://${System.getProperty("user.home")}/.colima/docker.sock")
	}
}
// end::docker-host[]

tasks.register("bootBuildImageDocker") {
	doFirst {
		println("host=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.host.get()}")
	}
}
