import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

// tag::configure-bom[]
plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}" apply false
	id("io.spring.dependency-management") version "{version-dependency-management-plugin}"
}

dependencyManagement {
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
