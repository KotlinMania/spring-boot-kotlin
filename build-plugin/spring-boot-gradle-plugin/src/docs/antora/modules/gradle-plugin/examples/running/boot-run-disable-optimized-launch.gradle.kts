import io.github.kotlinmania.spring.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::launch[]
tasks.named<BootRun>("bootRun") {
	optimizedLaunch.set(false)
}
// end::launch[]

tasks.register("optimizedLaunch") {
	doLast {
		println(tasks.getByName<BootRun>("bootRun").optimizedLaunch.get())
	}
}
