plugins {
	war
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

apply(plugin = "io.spring.dependency-management")

// tag::dependencies[]
dependencies {
	implementation("io.github.kotlinmania.spring.boot.spring-boot-starter-web")
	providedRuntime("io.github.kotlinmania.spring.boot.spring-boot-starter-tomcat-runtime")
}
// end::dependencies[]
