import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::caches[]
tasks.named<BootBuildImage>("bootBuildImage") {
	buildCache {
		volume {
			name.set("cache-${rootProject.name}.build")
		}
	}
	launchCache {
		volume {
			name.set("cache-${rootProject.name}.launch")
		}
	}
}
// end::caches[]

tasks.register("bootBuildImageCaches") {
	doFirst {
		println("buildCache=" + tasks.getByName<BootBuildImage>("bootBuildImage").buildCache.asCache()?.volume?.name)
		println("launchCache=" + tasks.getByName<BootBuildImage>("bootBuildImage").launchCache.asCache()?.volume?.name)
	}
}
