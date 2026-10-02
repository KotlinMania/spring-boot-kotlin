import io.github.kotlinmania.spring.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::source-resources[]
tasks.named<BootRun>("bootRun") {
	sourceResources(sourceSets["main"])
}
// end::source-resources[]

tasks.register("configuredClasspath") {
	doLast {
		println(tasks.getByName<BootRun>("bootRun").classpath.files)
	}
}
