import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::configure-bom[]
apply(plugin = "io.spring.dependency-management")

the<DependencyManagementExtension>().apply {
	imports {
		mavenBom(io.github.kotlinmania.spring.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
	}
}
// end::configure-bom[]

the<DependencyManagementExtension>().apply {
	resolutionStrategy {
		eachDependency {
			if (requested.group == "io.github.kotlinmania.spring.boot.) {
				useVersion("TEST-SNAPSHOT")
			}
		}
	}
}

repositories {
	maven {
		url = uri("repository")
	}
}
