import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootWar

plugins {
	war
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

tasks.named<BootWar>("bootWar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::properties-launcher[]
tasks.named<BootWar>("bootWar") {
	manifest {
		attributes("Main-Class" to "io.github.kotlinmania.spring.boot.loader.launch.PropertiesLauncher")
	}
}
// end::properties-launcher[]
