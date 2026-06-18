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
package org.springframework.boot.build.antora

import java.io.Serializable
import java.util.*
import java.util.List
import java.util.Map
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * Antora and Asciidoc extensions used by Spring Boot.
 * 
 * @author Phillip Webb
 */
object Extensions {
    private const val ROOT_COMPONENT_EXTENSION = "@springio/antora-extensions/root-component-extension"

    val antora: MutableList<Extension?>? = null

    init {
        val extensions: MutableList<Extension?> = ArrayList<Extension?>()
        extensions.add(
            Extensions.Extension(
                "@springio/antora-extensions", ROOT_COMPONENT_EXTENSION,
                "@springio/antora-extensions/static-page-extension",
                "@springio/antora-extensions/override-navigation-builder-extension"
            )
        )
        extensions.add(Extension("@springio/antora-xref-extension"))
        extensions.add(Extension("@springio/antora-zip-contents-collector-extension"))
        antora = List.copyOf<Extension?>(extensions)
    }

    val asciidoc: MutableList<Extension?>? = null

    init {
        val extensions: MutableList<Extension?> = ArrayList<Extension?>()
        extensions.add(Extension("@asciidoctor/tabs"))
        extensions.add(
            Extensions.Extension(
                "@springio/asciidoctor-extensions", "@springio/asciidoctor-extensions",
                "@springio/asciidoctor-extensions/javadoc-extension",
                "@springio/asciidoctor-extensions/configuration-properties-extension",
                "@springio/asciidoctor-extensions/section-ids-extension"
            )
        )
        asciidoc = List.copyOf<Extension?>(extensions)
    }

    val localOverrides = mutableMapOf<String?, String?>()

    fun antora(extensions: Consumer<AntoraExtensionsConfiguration?>): MutableList<MutableMap<String?, Any?>?> {
        val result = AntoraExtensionsConfiguration(
            antora!!.stream().flatMap<String> { obj: Extension? -> obj!!.names() }.sorted().toList()
        )
        extensions.accept(result)
        return result.config()
    }

    fun asciidoc(): MutableList<String?> {
        return asciidoc!!.stream().flatMap<String> { obj: Extension? -> obj!!.names() }.sorted().toList()
    }

    class Extension(val name: String?, vararg val includeNames: Array<String?>?) {
        fun names(): Stream<String?> {
            return if (this.includeNames.size != 0) Arrays.stream<String?>(this.includeNames) else Stream.of<String?>(
                this.name
            )
        }
    }

    class AntoraExtensionsConfiguration constructor(names: MutableList<String?>) {
        val extensions: MutableMap<String?, MutableMap<String?, Any?>?> =
            TreeMap<String?, MutableMap<String?, Any?>?>()

        init {
            names.forEach(Consumer { name: String? -> this.extensions.put(name, null) })
        }

        fun xref(xref: Consumer<Xref?>) {
            xref.accept(AntoraExtensionsConfiguration.Xref())
        }

        fun zipContentsCollector(zipContentsCollector: Consumer<ZipContentsCollector?>) {
            zipContentsCollector.accept(AntoraExtensionsConfiguration.ZipContentsCollector())
        }

        fun rootComponent(rootComponent: Consumer<RootComponent?>) {
            rootComponent.accept(RootComponent())
        }

        fun config(): MutableList<MutableMap<String?, Any?>?> {
            val config: MutableList<MutableMap<String?, Any?>?> = ArrayList<MutableMap<String?, Any?>?>()
            val orderedExtensions: MutableMap<String?, MutableMap<String?, Any?>?> =
                LinkedHashMap<String?, MutableMap<String?, Any?>?>(this.extensions)
            // The root component extension must be last
            val rootComponentConfig = orderedExtensions.remove(ROOT_COMPONENT_EXTENSION)
            orderedExtensions.put(ROOT_COMPONENT_EXTENSION, rootComponentConfig)
            orderedExtensions.forEach { (name: String?, customizations: MutableMap<String?, Any?>?) ->
                val extensionConfig: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
                extensionConfig.put("require", localOverrides.getOrDefault(name, name))
                if (customizations != null) {
                    extensionConfig.putAll(customizations)
                }
                config.add(extensionConfig)
            }
            return List.copyOf<MutableMap<String?, Any?>?>(config)
        }

        abstract inner class Customizer(val name: String?) {
            protected fun customize(key: String?, value: Any?) {
                this@AntoraExtensionsConfiguration.extensions.computeIfAbsent(this.name) { name: kotlin.String? -> java.util.TreeMap<kotlin.String?, kotlin.Any?>() }!!
                    .put(key, value)
            }
        }

        inner class Xref : Customizer("@springio/antora-xref-extension") {
            fun stub(stub: MutableList<String?>?) {
                if (stub != null && !stub.isEmpty()) {
                    customize("stub", stub)
                }
            }
        }

        inner class ZipContentsCollector : Customizer("@springio/antora-zip-contents-collector-extension") {
            fun versionFile(versionFile: String?) {
                customize("version_file", versionFile)
            }

            fun locations(locations: MutableList<String?>?) {
                customize("locations", locations)
            }

            fun alwaysInclude(alwaysInclude: MutableList<AlwaysInclude?>?) {
                if (alwaysInclude != null && !alwaysInclude.isEmpty()) {
                    customize(
                        "always_include",
                        alwaysInclude.stream().map<MutableMap<String?, String?>?> { obj: AlwaysInclude? -> obj.asMap() }
                            .toList())
                }
            }

            @JvmRecord
            data class AlwaysInclude(val name: String?, val classifier: String?) : Serializable {
                fun asMap(): MutableMap<String?, String?> {
                    return TreeMap<String?, String?>(
                        Map.of<String?, String?>(
                            "name",
                            this.name,
                            "classifier",
                            this.classifier
                        )
                    )
                }
            }
        }

        inner class RootComponent : Customizer(ROOT_COMPONENT_EXTENSION) {
            fun name(name: String?) {
                customize("root_component_name", name)
            }
        }
    }
}
