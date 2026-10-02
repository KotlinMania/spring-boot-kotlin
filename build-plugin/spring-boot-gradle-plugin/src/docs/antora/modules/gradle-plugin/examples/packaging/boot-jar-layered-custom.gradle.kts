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
		application {
			intoLayer("spring-boot-loader") {
				include("io.github.kotlinmania.spring.boot.loader/**")
			}
			intoLayer("application")
		}
		dependencies {
			intoLayer("application") {
				includeProjectDependencies()
			}
			intoLayer("snapshot-dependencies") {
				include("*:*:*SNAPSHOT")
			}
			intoLayer("dependencies")
		}
		layerOrder.set(listOf("dependencies", "spring-boot-loader", "snapshot-dependencies", "application"))
	}
}
// end::layered[]
