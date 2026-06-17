/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.build.docs

import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.MinimalJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.springframework.boot.build.bom.ResolvedBom
import org.springframework.boot.build.bom.ResolvedBom.Companion.readFrom
import org.springframework.boot.build.bom.ResolvedBom.JavadocLink

/**
 * An [Action] to configure the links option of a [Javadoc] task.
 * 
 * @author Andy Wilkinson
 */
class ConfigureJavadocLinks(
    private val resolvedBoms: FileCollection,
    private val includedLibraries: MutableCollection<String?>
) : Action<Javadoc?> {
    override fun execute(javadoc: Javadoc) {
        javadoc.options(Action { options: MinimalJavadocOptions ->
            if (options is StandardJavadocDocletOptions) {
                configureLinks(options)
            }
        })
    }

    private fun configureLinks(options: StandardJavadocDocletOptions) {
        val resolvedBom = readFrom(this.resolvedBoms.singleFile)
        val links: MutableList<String?> = ArrayList<String?>()
        links.add("https://docs.oracle.com/en/java/javase/17/docs/api/")
        links.add("https://jakarta.ee/specifications/platform/11/apidocs/")
        resolvedBom!!.libraries!!
            .stream()
            .filter { candidate: ResolvedBom.ResolvedLibrary? -> this.includedLibraries.contains(candidate!!.name) }
            .flatMap<JavadocLink> { library: ResolvedBom.ResolvedLibrary? -> library!!.links!!.javadoc!!.stream() }
            .map<Any>(JavadocLink::uri)
            .map { obj: Any? -> obj.toString() }
            .forEach(links::add)
        options.setLinks(links)
    }
}
