import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::buildpacks[]
tasks.named<BootBuildImage>("bootBuildImage") {
	buildpacks.set(listOf("file:///path/to/example-buildpack.tgz", "urn:cnb:builder:paketo-buildpacks/java"))
}
// end::buildpacks[]

tasks.register("bootBuildImageBuildpacks") {
	doFirst {
		for(reference in tasks.getByName<BootBuildImage>("bootBuildImage").buildpacks.get()) {
			print(reference)
		}
	}
}
