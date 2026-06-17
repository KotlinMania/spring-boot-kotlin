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

import org.assertj.core.api.Assertions
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.build.antora.Extensions.AntoraExtensionsConfiguration.ZipContentsCollector.AlwaysInclude
import org.springframework.util.function.ThrowingConsumer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.List

/**
 * Tests for [GenerateAntoraPlaybook].
 * 
 * @author Phillip Webb
 */
internal class GenerateAntoraPlaybookTests {
    @TempDir
    var temp: File? = null

    @Test
    @Throws(Exception::class)
    fun writePlaybookGeneratesExpectedContent() {
        writePlaybookYml(ThrowingConsumer { task: GenerateAntoraPlaybook? ->
            task!!.antoraExtensions.xref.stubs!!.addAll("appendix:.*", "api:.*", "reference:.*")
            val zipContentsCollector = task.antoraExtensions.zipContentsCollector
            zipContentsCollector.alwaysInclude.this!!.set(
                List.< E > of < E ? > (AlwaysInclude(
                    "test",
                    "local-aggregate-content"
                ))
            )
            zipContentsCollector.dependencies.add("test-dependency")
        })
        val actual = Files.readString(
            this.temp!!.toPath()
                .resolve("rootproject/project/build/generated/docs/antora-playbook/antora-playbook.yml")
        )
        val expected = Files
            .readString(Path.of("src/test/resources/org/springframework/boot/build/antora/expected-playbook.yml"))
        Assertions.assertThat(actual.replace('\\', '/')).isEqualToNormalizingNewlines(expected.replace('\\', '/'))
    }

    @Test
    @Throws(Exception::class)
    fun writePlaybookWhenHasJavadocExcludeGeneratesExpectedContent() {
        writePlaybookYml(ThrowingConsumer { task: GenerateAntoraPlaybook? ->
            task!!.antoraExtensions.xref.stubs!!.addAll("appendix:.*", "api:.*", "reference:.*")
            val zipContentsCollector = task.antoraExtensions.zipContentsCollector
            zipContentsCollector.alwaysInclude.this!!.set(
                List.< E > of < E ? > (AlwaysInclude(
                    "test",
                    "local-aggregate-content"
                ))
            )
            zipContentsCollector.dependencies.add("test-dependency")
            task.asciidocExtensions.excludeJavadocExtension.set(true)
        })
        val actual = Files.readString(
            this.temp!!.toPath()
                .resolve("rootproject/project/build/generated/docs/antora-playbook/antora-playbook.yml")
        )
        Assertions.assertThat(actual).doesNotContain("javadoc-extension")
    }

    @Throws(Exception::class)
    private fun writePlaybookYml(customizer: ThrowingConsumer<GenerateAntoraPlaybook?>) {
        val rootProjectDir = File(this.temp, "rootproject").getCanonicalFile()
        rootProjectDir.mkdirs()
        val rootProject = ProjectBuilder.builder().withProjectDir(rootProjectDir).build()
        val projectDir = File(rootProjectDir, "project")
        projectDir.mkdirs()
        val project = ProjectBuilder.builder().withProjectDir(projectDir).withParent(rootProject).build()
        project.getTasks()
            .register<GenerateAntoraPlaybook?>(
                "generateAntoraPlaybook",
                GenerateAntoraPlaybook::class.java,
                org.gradle.api.Action { t: GenerateAntoraPlaybook? -> customizer.accept(t) })
            .get()!!
            .writePlaybookYml()
    }
}
