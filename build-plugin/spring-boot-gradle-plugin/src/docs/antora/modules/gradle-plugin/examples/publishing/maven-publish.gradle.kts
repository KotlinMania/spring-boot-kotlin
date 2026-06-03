plugins {
	java
	`maven-publish`
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::publishing[]
publishing {
	publications {
		create<MavenPublication>("bootJava") {
			artifact(tasks.named("bootJar"))
		}
	}
	repositories {
		maven {
			url = uri("https://repo.example.com")
		}
	}
}
// end::publishing[]

tasks.register("publishingConfiguration") {
	doLast {
		println(publishing.publications["bootJava"])
		println(publishing.repositories.getByName<MavenArtifactRepository>("maven").url)
	}
}
