import io.github.kotlinmania.spring.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("io.github.kotlinmania.spring.boot.) version "{version-spring-boot}"
}

// tag::caches[]
tasks.named<BootBuildImage>("bootBuildImage") {
	buildWorkspace {
		bind {
			source.set("/tmp/cache-${rootProject.name}.work")
		}
	}
	buildCache {
		bind {
			source.set("/tmp/cache-${rootProject.name}.build")
		}
	}
	launchCache {
		bind {
			source.set("/tmp/cache-${rootProject.name}.launch")
		}
	}
}
// end::caches[]

tasks.register("bootBuildImageCaches") {
	doFirst {
		println("buildWorkspace=" + tasks.getByName<BootBuildImage>("bootBuildImage").buildWorkspace.asCache()?.bind?.source)
		println("buildCache=" + tasks.getByName<BootBuildImage>("bootBuildImage").buildCache.asCache()?.bind?.source)
		println("launchCache=" + tasks.getByName<BootBuildImage>("bootBuildImage").launchCache.asCache()?.bind?.source)
	}
}
