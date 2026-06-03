plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::exclude-time[]
springBoot {
	buildInfo {
		excludes.set(setOf("time"))
	}
}
// end::exclude-time[]
