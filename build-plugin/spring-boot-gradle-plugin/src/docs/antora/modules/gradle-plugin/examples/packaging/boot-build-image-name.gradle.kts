import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::image-name[]
tasks.named<BootBuildImage>("bootBuildImage") {
	imageName.set("example.com/library/${project.name}")
}
// end::image-name[]

tasks.register("bootBuildImageName") {
	doFirst {
		println(tasks.getByName<BootBuildImage>("bootBuildImage").imageName.get())
	}
}
