import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::disable-jar[]
tasks.named<Jar>("jar") {
	enabled = false
}
// end::disable-jar[]

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.Application")
}
