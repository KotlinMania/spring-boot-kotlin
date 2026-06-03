import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootWar

plugins {
	war
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

tasks.named<BootWar>("bootWar") {
	mainClass.set("com.example.ExampleApplication")
}

dependencies {
	"developmentOnly"(files("spring-boot-devtools-1.2.3.RELEASE.jar"))
}

// tag::include-devtools[]
tasks.named<BootWar>("bootWar") {
	classpath(configurations["developmentOnly"])
}
// end::include-devtools[]
