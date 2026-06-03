plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::build-info[]
springBoot {
	buildInfo()
}
// end::build-info[]
