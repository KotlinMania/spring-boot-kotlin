import io.github.kotlinmania.spring.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::main[]
tasks.named<BootRun>("bootRun") {
	mainClass.set("com.example.ExampleApplication")
}
// end::main[]

tasks.register("configuredMainClass") {
	doLast {
		println(tasks.getByName<BootRun>("bootRun").mainClass.get())
	}
}
