import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::main-class[]
tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}
// end::main-class[]
