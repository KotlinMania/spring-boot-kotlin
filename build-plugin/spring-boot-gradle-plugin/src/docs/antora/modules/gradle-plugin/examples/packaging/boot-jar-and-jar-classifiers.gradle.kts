import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::classifiers[]
tasks.named<BootJar>("bootJar") {
	archiveClassifier.set("boot")
}

tasks.named<Jar>("jar") {
	archiveClassifier.set("")
}
// end::classifiers[]

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.Application")
}
