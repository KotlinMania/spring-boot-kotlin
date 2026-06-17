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
package org.springframework.boot.build.architecture

import org.assertj.core.api.Assertions
import org.gradle.api.tasks.SourceSet
import org.gradle.testkit.runner.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.build.architecture.annotations.*
import org.springframework.util.ClassUtils
import org.springframework.util.FileSystemUtils
import org.springframework.util.StringUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.function.UnaryOperator
import java.util.stream.Collectors

/**
 * Tests for [ArchitectureCheck].
 * 
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Ivan Malutin
 * @author Dmytro Nosan
 * @author Stefano Cordio
 */
internal class ArchitectureCheckTests {
    private var gradleBuild: GradleBuild? = null

    @BeforeEach
    fun setup(@TempDir projectDir: Path) {
        this.gradleBuild = GradleBuild(projectDir)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenPackagesAreTangledShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "tangled")
        buildAndFail(this.gradleBuild!!, task, "slices matching '(**)' should be free of cycles")
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenPackagesAreNotTangledShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "untangled")
        build(this.gradleBuild!!, task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenBeanPostProcessorBeanMethodIsNotStaticShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "bpp/nonstatic")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task,
            "methods that are annotated with @Bean and have raw return type assignable"
                    + " to org.springframework.beans.factory.config.BeanPostProcessor"
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenBeanPostProcessorBeanMethodIsStaticAndHasUnsafeParametersShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "bpp/unsafeparameters")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task,
            "methods that are annotated with @Bean and have raw return type assignable"
                    + " to org.springframework.beans.factory.config.BeanPostProcessor"
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenBeanPostProcessorBeanMethodIsStaticAndHasSafeParametersShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "bpp/safeparameters")
        build(this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenBeanPostProcessorBeanMethodIsStaticAndHasNoParametersShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "bpp/noparameters")
        build(this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenBeanFactoryPostProcessorBeanMethodIsNotStaticShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "bfpp/nonstatic")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task,
            "methods that are annotated with @Bean and have raw return type assignable"
                    + " to org.springframework.beans.factory.config.BeanFactoryPostProcessor"
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenBeanFactoryPostProcessorBeanMethodIsStaticAndHasParametersShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "bfpp/parameters")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task,
            "methods that are annotated with @Bean and have raw return type assignable"
                    + " to org.springframework.beans.factory.config.BeanFactoryPostProcessor"
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenBeanFactoryPostProcessorBeanMethodIsStaticAndHasNoParametersShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "bfpp/noparameters")
        build(this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassLoadsResourceUsingResourceUtilsShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "resources/loads")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task,
            "no classes should call method where target owner type"
                    + " org.springframework.util.ResourceUtils and target name 'getURL'"
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassUsesResourceUtilsWithoutLoadingResourcesShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "resources/noloads")
        build(this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassDoesNotCallObjectsRequireNonNullShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "objects/noRequireNonNull")
        build(this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsObjectsRequireNonNullWithMessageShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "objects/requireNonNullWithString")
        buildAndFail(this.gradleBuild!!, task, "no classes should call method Objects.requireNonNull(Object, String)")
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsObjectsRequireNonNullWithMessageAndProhibitObjectsRequireNonNullIsFalseShouldSucceedAndWriteEmptyReport(
        task: Task
    ) {
        prepareTask(task, "objects/requireNonNullWithString")
        build(this.gradleBuild!!.withProhibitObjectsRequireNonNull(false), task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsObjectsRequireNonNullWithSupplierShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "objects/requireNonNullWithSupplier")
        buildAndFail(this.gradleBuild!!, task, "no classes should call method Objects.requireNonNull(Object, Supplier)")
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsObjectsRequireNonNullWithSupplierAndProhibitObjectsRequireNonNullIsFalseShouldSucceedAndWriteEmptyReport(
        task: Task
    ) {
        prepareTask(task, "objects/requireNonNullWithSupplier")
        build(this.gradleBuild!!.withProhibitObjectsRequireNonNull(false), task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsCollectorsToListShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "collectors/toList")
        buildAndFail(this.gradleBuild!!, task, "because java.util.stream.Stream.toList() should be used instead")
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsUrlEncoderWithStringEncodingShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "url/encode")
        buildAndFail(
            this.gradleBuild!!, task,
            "because java.net.URLEncoder.encode(String s, Charset charset) should be used instead"
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsUrlDecoderWithStringEncodingShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "url/decode")
        buildAndFail(
            this.gradleBuild!!, task,
            "because java.net.URLDecoder.decode(String s, Charset charset) should be used instead"
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsStringToUpperCaseWithoutLocaleShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "string/toUpperCase")
        buildAndFail(this.gradleBuild!!, task, "because String.toUpperCase(Locale.ROOT) should be used instead")
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsStringToLowerCaseWithoutLocaleShouldFailAndWriteReport(task: Task) {
        prepareTask(task, "string/toLowerCase")
        buildAndFail(this.gradleBuild!!, task, "because String.toLowerCase(Locale.ROOT) should be used instead")
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsStringToLowerCaseWithLocaleShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "string/toLowerCaseWithLocale")
        build(this.gradleBuild!!, task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenClassCallsStringToUpperCaseWithLocaleShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "string/toUpperCaseWithLocale")
        build(this.gradleBuild!!, task)
    }

    @Test
    @Throws(IOException::class)
    fun whenConditionalOnMissingBeanWithTypeSameAsMethodReturnTypeShouldFailAndWriteReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "conditionalonmissingbean/valueonly", "annotations")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConditionalOnMissingBeanAnnotation(),
            Task.CHECK_ARCHITECTURE_MAIN,
            "should not specify only a value that is the same as the method's return type"
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenConditionalOnMissingBeanWithTypeAttributeShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "conditionalonmissingbean/withtype", "annotations")
        build(this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenConditionalOnMissingBeanWithNameAttributeShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "conditionalonmissingbean/withname", "annotations")
        build(this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task)
    }

    @Test
    @Throws(IOException::class)
    fun whenClassLevelConfigurationPropertiesContainsOnlyPrefixShouldFailAndWriteReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "configurationproperties/classprefixonly", "annotations")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConfigurationPropertiesAnnotation(),
            Task.CHECK_ARCHITECTURE_MAIN,
            "should specify implicit 'value' attribute other than explicit 'prefix' attribute"
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenClassLevelConfigurationPropertiesContainsPrefixAndIgnoreShouldSucceedAndWriteEmptyReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "configurationproperties/classprefixandignore", "annotations")
        build(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConfigurationPropertiesAnnotation(),
            Task.CHECK_ARCHITECTURE_MAIN
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenClassLevelConfigurationPropertiesContainsOnlyValueShouldSucceedAndWriteEmptyReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "configurationproperties/classvalueonly", "annotations")
        build(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConfigurationPropertiesAnnotation(),
            Task.CHECK_ARCHITECTURE_MAIN
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenMethodLevelConfigurationPropertiesContainsOnlyPrefixShouldFailAndWriteReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "configurationproperties/methodprefixonly", "annotations")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConfigurationPropertiesAnnotation(),
            Task.CHECK_ARCHITECTURE_MAIN,
            "should specify implicit 'value' attribute other than explicit 'prefix' attribute"
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenMethodLevelConfigurationPropertiesContainsPrefixAndIgnoreShouldSucceedAndWriteEmptyReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "configurationproperties/methodprefixandignore", "annotations")
        build(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConfigurationPropertiesAnnotation(),
            Task.CHECK_ARCHITECTURE_MAIN
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenMethodLevelConfigurationPropertiesContainsOnlyValueShouldSucceedAndWriteEmptyReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "configurationproperties/methodvalueonly", "annotations")
        build(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConfigurationPropertiesAnnotation(),
            Task.CHECK_ARCHITECTURE_MAIN
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenConfigurationPropertiesBindingBeanMethodIsNotStaticShouldFailAndWriteReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "configurationproperties/bindingnonstatic", "annotations")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConfigurationPropertiesBindingAnnotation(),
            Task.CHECK_ARCHITECTURE_MAIN, "does not have modifier STATIC"
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenBeanPostProcessorBeanMethodIsNotStaticWithExternalClassShouldFailAndWriteReport(task: Task) {
        val sourceDirectory = task.getSourceDirectory(this.gradleBuild!!.projectDir)
            .resolve(ClassUtils.classPackageAsResourcePath(javaClass))
        Files.createDirectories(sourceDirectory)
        Files.writeString(
            sourceDirectory.resolve("TestClass.java"), """
				package %s;
				import org.springframework.context.annotation.Bean;
				import org.springframework.integration.monitor.IntegrationMBeanExporter;
				public class TestClass {
					@Bean
					IntegrationMBeanExporter integrationMBeanExporter() {
						return new IntegrationMBeanExporter();
					}
				}
				
				""".trimIndent().formatted(ClassUtils.getPackageName(javaClass))
        )
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_INTEGRATION_JMX), task,
            "methods that are annotated with @Bean and have raw return type assignable "
                    + "to org.springframework.beans.factory.config.BeanPostProcessor"
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenBeanMethodExposesPrivateTypeWithMainSourcesShouldFailAndWriteReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "beans/privatebean")
        buildAndFail(
            this.gradleBuild!!.withDependencies(SPRING_CONTEXT), Task.CHECK_ARCHITECTURE_MAIN,
            "methods that are annotated with @Bean should not return types declared "
                    + "with the PRIVATE modifier, as such types are incompatible with Spring AOT processing",
            "returns Class <org.springframework.boot.build.architecture.beans.privatebean.PrivateBean\$MyBean>"
                    + " which is declared as [PRIVATE, STATIC, FINAL]"
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenBeanMethodExposesPrivateTypeWithTestsSourcesShouldSucceedAndWriteEmptyReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_TEST, "beans/privatebean")
        build(this.gradleBuild!!.withDependencies(SPRING_CONTEXT), Task.CHECK_ARCHITECTURE_TEST)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Task::class)
    @Throws(IOException::class)
    fun whenBeanMethodExposesNonPrivateTypeShouldSucceedAndWriteEmptyReport(task: Task) {
        prepareTask(task, "beans/regular")
        build(this.gradleBuild!!.withDependencies(SPRING_CONTEXT), task)
    }

    @Test
    @Throws(IOException::class)
    fun whenEnumSourceValueIsInferredShouldSucceedAndWriteEmptyReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_TEST, "junit/enumsource/inferredfromparametertype")
        build(this.gradleBuild!!.withDependencies(JUNIT_JUPITER), Task.CHECK_ARCHITECTURE_TEST)
    }

    @Test
    @Throws(IOException::class)
    fun whenEnumSourceValueIsNotTheSameAsTypeOfMethodsFirstParameterShouldSucceedAndWriteEmptyReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_TEST, "junit/enumsource/valuenecessary")
        build(this.gradleBuild!!.withDependencies(JUNIT_JUPITER), Task.CHECK_ARCHITECTURE_TEST)
    }

    @Test
    @Throws(IOException::class)
    fun whenEnumSourceValueIsSameAsTypeOfMethodsFirstParameterShouldFailAndWriteReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_TEST, "junit/enumsource/sameasparametertype")
        buildAndFail(
            this.gradleBuild!!.withDependencies(JUNIT_JUPITER), Task.CHECK_ARCHITECTURE_TEST,
            ("method <org.springframework.boot.build.architecture.junit.enumsource.sameasparametertype"
                    + ".EnumSourceSameAsParameterType.exampleMethod(org.springframework.boot.build."
                    + "architecture.junit.enumsource.sameasparametertype.EnumSourceSameAsParameterType\$Example)>"),
            "should not have a value that is the same as the type of the method's first parameter"
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenConditionalOnClassUsedOnBeanMethodsWithMainSourcesShouldFailAndWriteReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "conditionalonclass", "annotations")
        val gradleBuild = this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConditionalOnClassAnnotation()
        buildAndFail(
            gradleBuild, Task.CHECK_ARCHITECTURE_MAIN,
            ("because @ConditionalOnClass on @Bean methods is ineffective - it doesn't prevent"
                    + " the method signature from being loaded. Such condition need to be placed"
                    + " on a @Configuration class, allowing the condition to back off before the type is loaded")
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenConditionalOnClassUsedOnBeanMethodsWithTestSourcesShouldSucceedAndWriteEmptyReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_TEST, "conditionalonclass", "annotations")
        val gradleBuild = this.gradleBuild!!.withDependencies(SPRING_CONTEXT).withConditionalOnClassAnnotation()
        build(gradleBuild, Task.CHECK_ARCHITECTURE_TEST)
    }

    @Test
    @Throws(IOException::class)
    fun whenDeprecatedConfigurationPropertyIsMissingSinceShouldFailAndWriteReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "configurationproperties/deprecatedsince", "annotations")
        val gradleBuild = this.gradleBuild!!.withDependencies(SPRING_CONTEXT)
            .withDeprecatedConfigurationPropertyAnnotation()
        buildAndFail(
            gradleBuild, Task.CHECK_ARCHITECTURE_MAIN,
            "should include a non-empty 'since' attribute of @DeprecatedConfigurationProperty",
            "DeprecatedConfigurationPropertySince.getProperty"
        )
    }

    @Test
    @Throws(IOException::class)
    fun whenCustomAssertionMethodNotReturningSelfIsAnnotatedWithCheckReturnValueShouldSucceedAndWriteEmptyReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "assertj/checkReturnValue")
        build(this.gradleBuild!!.withDependencies(ASSERTJ_CORE, SPRING_CORE), Task.CHECK_ARCHITECTURE_MAIN)
    }

    @Test
    @Throws(IOException::class)
    fun whenCustomAssertionMethodNotReturningSelfIsNotAnnotatedWithCheckReturnValueShouldFailAndWriteReport() {
        prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "assertj/noCheckReturnValue")
        buildAndFail(
            this.gradleBuild!!.withDependencies(ASSERTJ_CORE), Task.CHECK_ARCHITECTURE_MAIN,
            ("methods that are declared in classes that implement org.assertj.core.api.Assert and "
                    + "are public and do not have modifier BRIDGE and do not return self type should be annotated "
                    + "with @CheckReturnValue")
        )
    }

    @Throws(IOException::class)
    private fun prepareTask(task: Task, vararg sourceDirectories: String) {
        for (sourceDirectory in sourceDirectories) {
            FileSystemUtils.copyRecursively(
                Paths.get("src/test/java")
                    .resolve(ClassUtils.classPackageAsResourcePath(javaClass))
                    .resolve(sourceDirectory),
                task.getSourceDirectory(this.gradleBuild!!.projectDir)
                    .resolve(ClassUtils.classPackageAsResourcePath(javaClass))
                    .resolve(sourceDirectory)
            )
        }
    }

    @Throws(IOException::class)
    private fun build(gradleBuild: GradleBuild, task: Task) {
        try {
            val buildResult = gradleBuild.build(task.toString())
            Assertions.assertThat<String?>(buildResult.taskPaths(TaskOutcome.SUCCESS)).`as`(buildResult.getOutput())
                .contains(":" + task)
            Assertions.assertThat(task.getFailureReport(gradleBuild.projectDir)).isEmpty()
        } catch (ex: UnexpectedBuildFailure) {
            val message = StringBuilder("Expected build to succeed but it failed")
            if (Files.exists(task.getFailureReportFile(gradleBuild.projectDir))) {
                message.append('\n').append(task.getFailureReport(gradleBuild.projectDir))
            }
            message.append('\n').append(ex.getBuildResult().getOutput())
            throw AssertionError(message.toString(), ex)
        }
    }

    @Throws(IOException::class)
    private fun buildAndFail(gradleBuild: GradleBuild, task: Task, vararg messages: String?) {
        try {
            val buildResult = gradleBuild.buildAndFail(task.toString())
            Assertions.assertThat<String?>(buildResult.taskPaths(TaskOutcome.FAILED)).`as`(buildResult.getOutput())
                .contains(":" + task)
            try {
                Assertions.assertThat(task.getFailureReport(gradleBuild.projectDir)).contains(*messages)
            } catch (ex: NoSuchFileException) {
                throw AssertionError("Expected failure report not found\n" + buildResult.getOutput())
            }
        } catch (ex: UnexpectedBuildSuccess) {
            throw AssertionError("Expected build to fail but it succeeded\n" + ex.getBuildResult().getOutput(), ex)
        }
    }

    private enum class Task(private val sourceSetName: String) {
        CHECK_ARCHITECTURE_MAIN(SourceSet.MAIN_SOURCE_SET_NAME),

        CHECK_ARCHITECTURE_TEST(SourceSet.TEST_SOURCE_SET_NAME);

        @Throws(IOException::class)
        fun getFailureReport(projectDir: Path): String {
            return Files.readString(getFailureReportFile(projectDir), StandardCharsets.UTF_8)
        }

        fun getFailureReportFile(projectDir: Path): Path {
            return projectDir.resolve("build/%s/failure-report.txt".formatted(toString()))
        }

        fun getSourceDirectory(projectDir: Path): Path {
            return projectDir.resolve("src/%s/java".formatted(this.sourceSetName))
        }

        override fun toString(): String {
            return "checkArchitecture" + StringUtils.capitalize(this.sourceSetName) + "Java"
        }
    }

    private class GradleBuild(val projectDir: Path) {
        private val dependencies: MutableSet<String?> = LinkedHashSet<String?>()

        private val taskConfigurations: MutableMap<Task?, TaskConfiguration?> =
            LinkedHashMap<Task?, TaskConfiguration?>()

        fun withProhibitObjectsRequireNonNull(prohibitObjectsRequireNonNull: Boolean?): GradleBuild {
            for (task in Task.entries) {
                configureTask(task, UnaryOperator { configuration: TaskConfiguration? ->
                    configuration!!
                        .withProhibitObjectsRequireNonNull(prohibitObjectsRequireNonNull)
                })
            }
            return this
        }

        fun withConditionalOnClassAnnotation(): GradleBuild {
            configureTasks(
                ArchitectureCheckAnnotation.CONDITIONAL_ON_CLASS.name,
                TestConditionalOnClass::class.java.getName()
            )
            return this
        }

        fun withConditionalOnMissingBeanAnnotation(): GradleBuild {
            configureTasks(
                ArchitectureCheckAnnotation.CONDITIONAL_ON_MISSING_BEAN.name,
                TestConditionalOnMissingBean::class.java.getName()
            )
            return this
        }

        fun withConfigurationPropertiesAnnotation(): GradleBuild {
            configureTasks(
                ArchitectureCheckAnnotation.CONFIGURATION_PROPERTIES.name,
                TestConfigurationProperties::class.java.getName()
            )
            return this
        }

        fun withConfigurationPropertiesBindingAnnotation(): GradleBuild {
            configureTasks(
                ArchitectureCheckAnnotation.CONFIGURATION_PROPERTIES_BINDING.name,
                TestConfigurationPropertiesBinding::class.java.getName()
            )
            return this
        }

        fun withDeprecatedConfigurationPropertyAnnotation(): GradleBuild {
            configureTasks(
                ArchitectureCheckAnnotation.DEPRECATED_CONFIGURATION_PROPERTY.name,
                TestDeprecatedConfigurationProperty::class.java.getName()
            )
            return this
        }

        fun configureTasks(annotationName: String?, annotationClass: String?) {
            for (task in Task.entries) {
                configureTask(
                    task,
                    UnaryOperator { configuration: TaskConfiguration? ->
                        configuration!!.withAnnotation(
                            annotationName,
                            annotationClass
                        )
                    })
            }
        }

        fun configureTask(task: Task?, configurer: UnaryOperator<TaskConfiguration?>) {
            this.taskConfigurations.computeIfAbsent(task) { key: Task? -> TaskConfiguration(null, null) }
            this.taskConfigurations.compute(task) { key: Task?, value: TaskConfiguration? -> configurer.apply(value) }
        }

        fun withDependencies(vararg dependencies: String?): GradleBuild {
            this.dependencies.clear()
            this.dependencies.addAll(Arrays.asList<String?>(*dependencies))
            return this
        }

        @Throws(IOException::class)
        fun build(vararg arguments: String?): BuildResult {
            return prepareRunner(*arguments)!!.build()
        }

        @Throws(IOException::class)
        fun buildAndFail(vararg arguments: String?): BuildResult {
            return prepareRunner(*arguments)!!.buildAndFail()
        }

        @Throws(IOException::class)
        fun prepareRunner(vararg arguments: String?): GradleRunner? {
            val buildFile = StringBuilder()
            buildFile.append("plugins {\n")
                .append("    id 'java'\n")
                .append("    id 'org.springframework.boot.architecture'\n")
                .append("}\n\n")
                .append("repositories {\n")
                .append("    mavenCentral()\n")
                .append("}\n\n")
                .append("java {\n")
                .append("    sourceCompatibility = '17'\n")
                .append("    targetCompatibility = '17'\n")
                .append("}\n\n")
            if (!this.dependencies.isEmpty()) {
                buildFile.append("dependencies {\n")
                for (dependency in this.dependencies) {
                    buildFile.append("\n    implementation ").append(StringUtils.quote(dependency))
                }
                buildFile.append("\n}\n\n")
            }
            this.taskConfigurations.forEach { (task: Task?, configuration: TaskConfiguration?) ->
                buildFile.append(task).append(" {")
                if (configuration!!.prohibitObjectsRequireNonNull != null) {
                    buildFile.append("\n    prohibitObjectsRequireNonNull = ")
                        .append(configuration.prohibitObjectsRequireNonNull)
                }
                if (configuration.annotations != null && !configuration.annotations.isEmpty()) {
                    buildFile.append("\n    annotationClasses = ")
                        .append(toGroovyMapString(configuration.annotations))
                }
                buildFile.append("\n}\n")
            }
            Files.writeString(this.projectDir.resolve("build.gradle"), buildFile, StandardCharsets.UTF_8)
            return GradleRunner.create()
                .withProjectDir(this.projectDir.toFile())
                .withArguments(*arguments)
                .withPluginClasspath()
        }

        private class TaskConfiguration(
            val prohibitObjectsRequireNonNull: Boolean?,
            annotations: MutableMap<String?, String?>?
        ) {
            fun withProhibitObjectsRequireNonNull(prohibitObjectsRequireNonNull: Boolean?): TaskConfiguration {
                return TaskConfiguration(prohibitObjectsRequireNonNull, this.annotations)
            }

            fun withAnnotation(name: String?, annotationClass: String?): TaskConfiguration {
                val map: MutableMap<String?, String?> = HashMap<String?, String?>(this.annotations)
                map.put(name, annotationClass)
                return TaskConfiguration(this.prohibitObjectsRequireNonNull, map)
            }

            val annotations: MutableMap<String?, String?>?

            init {
                var annotations = annotations
                if (annotations == null) {
                    annotations = HashMap<String?, String?>()
                }
                this.annotations = annotations
            }
        }

        companion object {
            fun toGroovyMapString(map: MutableMap<String?, String?>): String {
                return map.entries
                    .stream()
                    .map<String?> { entry: MutableMap.MutableEntry<String?, String?>? -> "'" + entry!!.key + "' : '" + entry.value + "'" }
                    .collect(Collectors.joining(", ", "[", "]"))
            }
        }
    }

    companion object {
        private const val ASSERTJ_CORE = "org.assertj:assertj-core:3.27.4"

        private const val JUNIT_JUPITER = "org.junit.jupiter:junit-jupiter:5.12.0"

        private const val SPRING_CONTEXT = "org.springframework:spring-context:6.2.15"

        private const val SPRING_CORE = "org.springframework:spring-core:6.2.15"

        private const val SPRING_INTEGRATION_JMX = "org.springframework.integration:spring-integration-jmx:6.5.1"
    }
}
