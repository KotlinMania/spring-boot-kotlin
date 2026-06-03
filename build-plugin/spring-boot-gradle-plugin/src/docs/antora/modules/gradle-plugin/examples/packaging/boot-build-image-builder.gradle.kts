import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootJar
import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::builder[]
tasks.named<BootBuildImage>("bootBuildImage") {
	builder.set("mine/java-cnb-builder")
	runImage.set("mine/java-cnb-run")
}
// end::builder[]

tasks.register("bootBuildImageBuilder") {
	doFirst {
		println("builder=${tasks.getByName<BootBuildImage>("bootBuildImage").builder.get()}")
		println("runImage=${tasks.getByName<BootBuildImage>("bootBuildImage").runImage.get()}")
	}
}
