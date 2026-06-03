plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

apply(plugin = "io.spring.dependency-management")

// tag::dependencies[]
dependencies {
	implementation("io.github.kotlinmania.spring.boot.spring-boot-starter-web")
	implementation("io.github.kotlinmania.spring.boot.spring-boot-starter-data-jpa")
}
// end::dependencies[]
