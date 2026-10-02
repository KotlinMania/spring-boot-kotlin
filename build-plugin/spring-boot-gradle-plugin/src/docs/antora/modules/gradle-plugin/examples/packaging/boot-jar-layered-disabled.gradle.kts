import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::layered[]
tasks.named<BootJar>("bootJar") {
	layered {
		enabled.set(false)
	}
}
// end::layered[]
