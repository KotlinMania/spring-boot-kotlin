plugins {
	java
	application
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::main-class[]
application {
	mainClass.set("com.example.ExampleApplication")
}
// end::main-class[]
