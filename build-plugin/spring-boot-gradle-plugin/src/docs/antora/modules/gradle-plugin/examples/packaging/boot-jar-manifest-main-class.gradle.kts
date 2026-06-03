import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::main-class[]
tasks.named<BootJar>("bootJar") {
	manifest {
		attributes("Start-Class" to "com.example.ExampleApplication")
	}
}
// end::main-class[]
