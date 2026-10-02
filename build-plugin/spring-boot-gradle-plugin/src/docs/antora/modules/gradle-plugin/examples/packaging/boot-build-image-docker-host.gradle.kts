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
		host.set("tcp://192.168.99.100:2376")
		tlsVerify.set(true)
		certPath.set("/home/user/.minikube/certs")
	}
}
// end::docker-host[]

tasks.register("bootBuildImageDocker") {
	doFirst {
		println("host=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.host.get()}")
		println("tlsVerify=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.tlsVerify.get()}")
		println("certPath=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.certPath.get()}")
	}
}
