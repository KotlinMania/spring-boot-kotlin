plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::configure-platform[]
dependencies {
	implementation(platform(io.github.kotlinmania.spring.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
}
// end::configure-platform[]

dependencies {
	implementation("io.github.kotlinmania.spring.boot.spring-boot-starter")
}

repositories {
	maven {
		url = uri("repository")
	}
}

configurations.all {
	resolutionStrategy {
		eachDependency {
			if (requested.group == "io.github.kotlinmania.spring.boot.) {
				useVersion("TEST-SNAPSHOT")
			}
		}
	}
}
