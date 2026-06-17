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

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.*
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitCallTarget
import com.tngtech.archunit.core.domain.properties.*
import com.tngtech.archunit.lang.*
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.lang.syntax.elements.ClassesShould
import com.tngtech.archunit.lang.syntax.elements.GivenMethodsConjunction
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Role
import org.springframework.lang.CheckReturnValue
import org.springframework.util.ResourceUtils
import java.lang.Deprecated
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.List
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.String
import kotlin.plus

/**
 * Factory used to create [architecture rules][ArchRule].
 * 
 * @author Andy Wilkinson
 * @author Yanming Zhou
 * @author Scott Frederick
 * @author Ivan Malutin
 * @author Phillip Webb
 * @author Ngoc Nhan
 * @author Moritz Halbritter
 * @author Stefano Cordio
 */
object ArchitectureRules {
    private const val AUTOCONFIGURATION_ANNOTATION = "org.springframework.boot.autoconfigure.AutoConfiguration"

    fun noClassesShouldCallObjectsRequireNonNull(): MutableList<ArchRule?> {
        return List.of<ArchRule?>(
            noClassesShould()!!.callMethod(Objects::class.java, "requireNonNull", Any::class.java, String::class.java)
                .because(shouldUse("org.springframework.utils.Assert.notNull(Object, String)")),
            noClassesShould()!!.callMethod(Objects::class.java, "requireNonNull", Any::class.java, Supplier::class.java)
                .because(shouldUse("org.springframework.utils.Assert.notNull(Object, Supplier)"))
        )
    }

    fun standard(): MutableList<ArchRule?> {
        val rules: MutableList<ArchRule?> = ArrayList<ArchRule?>()
        rules.add(allPackagesShouldBeFreeOfTangles())
        rules.add(allBeanPostProcessorBeanMethodsShouldBeStaticAndNotCausePrematureInitialization())
        rules.add(allBeanFactoryPostProcessorBeanMethodsShouldBeStaticAndHaveOnlyInjectEnvironment())
        rules.add(noClassesShouldCallStepVerifierStepVerifyComplete())
        rules.add(noClassesShouldConfigureDefaultStepVerifierTimeout())
        rules.add(noClassesShouldCallCollectorsToList())
        rules.add(noClassesShouldCallURLEncoderWithStringEncoding())
        rules.add(noClassesShouldCallURLDecoderWithStringEncoding())
        rules.add(noClassesShouldLoadResourcesUsingResourceUtils())
        rules.add(noClassesShouldCallStringToUpperCaseWithoutLocale())
        rules.add(noClassesShouldCallStringToLowerCaseWithoutLocale())
        rules.add(enumSourceShouldNotHaveValueThatIsTheSameAsTypeOfMethodsFirstParameter())
        rules.add(conditionsShouldNotBePublic())
        rules.add(autoConfigurationClassesShouldBePublicAndFinal())
        rules.add(autoConfigurationClassesShouldHaveNoPublicMembers())
        rules.add(testAutoConfigurationClassesShouldBePackagePrivateAndFinal())
        return List.copyOf<ArchRule?>(rules)
    }

    fun beanMethods(annotationClass: String?): MutableList<ArchRule?> {
        return List.of<ArchRule?>(
            allBeanMethodsShouldReturnNonPrivateType(),
            allBeanMethodsShouldNotHaveConditionalOnClassAnnotation(annotationClass)
        )
    }

    fun conditionalOnMissingBean(annotationClass: String?): MutableList<ArchRule?> {
        return List
            .of<ArchRule?>(
                conditionalOnMissingBeanShouldNotSpecifyOnlyATypeThatIsTheSameAsMethodReturnType(
                    annotationClass
                )
            )
    }

    fun configurationProperties(annotationClass: String?): MutableList<ArchRule?> {
        return List.of<ArchRule?>(
            classLevelConfigurationPropertiesShouldNotSpecifyOnlyPrefixAttribute(annotationClass),
            methodLevelConfigurationPropertiesShouldNotSpecifyOnlyPrefixAttribute(annotationClass)
        )
    }

    fun configurationPropertiesBinding(annotationClass: String?): MutableList<ArchRule?> {
        return List.of<ArchRule?>(allConfigurationPropertiesBindingBeanMethodsShouldBeStatic(annotationClass))
    }

    fun configurationPropertiesDeprecation(annotationClass: String?): MutableList<ArchRule?> {
        return List.of<ArchRule?>(allDeprecatedConfigurationPropertiesShouldIncludeSince(annotationClass))
    }

    private fun allBeanMethodsShouldReturnNonPrivateType(): ArchRule? {
        return methodsThatAreAnnotatedWith("org.springframework.context.annotation.Bean")!!.should(
            check<JavaMethod?>(
                "not return types declared with the %s modifier, as such types are incompatible with Spring AOT processing"
                    .format(JavaModifier.PRIVATE),
                BiConsumer { method: JavaMethod?, events: ConditionEvents? ->
                    val returnType = method!!.getRawReturnType()
                    if (returnType.getModifiers().contains(JavaModifier.PRIVATE)) {
                        ArchitectureRules.addViolation(
                            events!!, method, "%s returns %s which is declared as %s".format(
                                method.description, returnType.description, returnType.getModifiers()
                            )
                        )
                    }
                })
        )
            .allowEmptyShould(true)
    }

    private fun allBeanMethodsShouldNotHaveConditionalOnClassAnnotation(annotationName: String?): ArchRule? {
        return methodsThatAreAnnotatedWith("org.springframework.context.annotation.Bean")!!.should()
            .notBeAnnotatedWith(annotationName)
            .because(
                ("@ConditionalOnClass on @Bean methods is ineffective - it doesn't prevent "
                        + "the method signature from being loaded. Such condition need to be placed"
                        + " on a @Configuration class, allowing the condition to back off before the type is loaded.")
            )
            .allowEmptyShould(true)
    }

    fun allCustomAssertionMethodsNotReturningSelfShouldBeAnnotatedWithCheckReturnValue(): ArchRule? {
        return ArchRuleDefinition.methods()
            .that()
            .areDeclaredInClassesThat()
            .implement("org.assertj.core.api.Assert")
            .and()
            .arePublic()
            .and()
            .doNotHaveModifier(JavaModifier.BRIDGE)
            .and(doNotReturnSelfType())
            .should()
            .beAnnotatedWith(CheckReturnValue::class.java)
            .allowEmptyShould(true)
    }

    private fun doNotReturnSelfType(): DescribedPredicate<JavaMethod?>? {
        return DescribedPredicate.describe<JavaMethod?>(
            "do not return self type",
            Predicate { method: JavaMethod? -> method!!.getRawReturnType() != method.getOwner() })
    }

    private fun allPackagesShouldBeFreeOfTangles(): ArchRule {
        return SlicesRuleDefinition.slices()
            .matching("(**)")
            .should()
            .beFreeOfCycles()
            .ignoreDependency(
                "org.springframework.boot.env.EnvironmentPostProcessor",
                "org.springframework.boot.SpringApplication"
            )
    }

    private fun allBeanPostProcessorBeanMethodsShouldBeStaticAndNotCausePrematureInitialization(): ArchRule? {
        return methodsThatAreAnnotatedWith("org.springframework.context.annotation.Bean")!!.and()
            .haveRawReturnType(assignableTo("org.springframework.beans.factory.config.BeanPostProcessor"))
            .should(onlyHaveParametersThatWillNotCauseEagerInitialization())
            .andShould()
            .beStatic()
            .allowEmptyShould(true)
    }

    private fun onlyHaveParametersThatWillNotCauseEagerInitialization(): ArchCondition<JavaMethod?> {
        return check<JavaMethod?>(
            "not have parameters that will cause eager initialization",
            BiConsumer { obj: JavaMethod?, item: ConditionEvents? ->
                ArchitectureRules.allBeanPostProcessorBeanMethodsShouldBeStaticAndNotCausePrematureInitialization(
                    item
                )
            })
    }

    private fun allBeanPostProcessorBeanMethodsShouldBeStaticAndNotCausePrematureInitialization(
        item: JavaMethod,
        events: ConditionEvents
    ) {
        val notAnnotatedWithLazy = DescribedPredicate
            .not<JavaParameter?>(CanBeAnnotated.Predicates.annotatedWith("org.springframework.context.annotation.Lazy"))
        val notOfASafeType = notAssignableTo(
            "org.springframework.beans.factory.ObjectProvider", "org.springframework.context.ApplicationContext",
            "org.springframework.core.env.Environment"
        )
            .and(notAnnotatedWithRoleInfrastructure())
        item.parameters
            .stream()
            .filter(notAnnotatedWithLazy)
            .filter { parameter: JavaParameter? -> notOfASafeType.test(parameter!!.getRawType()) }
            .forEach { parameter: JavaParameter? ->
                addViolation(
                    events, parameter,
                    (parameter!!.description + " will cause eager initialization as it is "
                            + notAnnotatedWithLazy.description + " and is " + notOfASafeType.description)
                )
            }
    }

    private fun notAnnotatedWithRoleInfrastructure(): DescribedPredicate<JavaClass?> {
        return `is`("not annotated with @Role(BeanDefinition.ROLE_INFRASTRUCTURE", Predicate { candidate: JavaClass? ->
            if (!candidate!!.isAnnotatedWith(Role::class.java)) {
                return@`is` true
            }
            val role = candidate.getAnnotationOfType<Role>(Role::class.java)
            role.value != BeanDefinition.ROLE_INFRASTRUCTURE
        })
    }

    private fun allBeanFactoryPostProcessorBeanMethodsShouldBeStaticAndHaveOnlyInjectEnvironment(): ArchRule? {
        return methodsThatAreAnnotatedWith("org.springframework.context.annotation.Bean")!!.and()
            .haveRawReturnType(assignableTo("org.springframework.beans.factory.config.BeanFactoryPostProcessor"))
            .should(onlyInjectEnvironment())
            .andShould()
            .beStatic()
            .allowEmptyShould(true)
    }

    private fun onlyInjectEnvironment(): ArchCondition<JavaMethod?> {
        return check<JavaMethod?>(
            "only inject Environment",
            BiConsumer { obj: JavaMethod?, item: ConditionEvents? -> ArchitectureRules.onlyInjectEnvironment(item) })
    }

    private fun onlyInjectEnvironment(item: JavaMethod, events: ConditionEvents) {
        if (item.parameters.stream().anyMatch { obj: JavaParameter? -> ArchitectureRules.isNotEnvironment() }) {
            addViolation(events, item, item.description + " should only inject Environment")
        }
    }

    private fun isNotEnvironment(parameter: JavaParameter): Boolean {
        return "org.springframework.core.env.Environment" != parameter.type.name
    }

    private fun noClassesShouldCallStepVerifierStepVerifyComplete(): ArchRule? {
        return noClassesShould()!!.callMethod("reactor.test.StepVerifier\$Step", "verifyComplete")
            .because("it can block indefinitely and " + shouldUse("expectComplete().verify(Duration)"))
    }

    private fun noClassesShouldConfigureDefaultStepVerifierTimeout(): ArchRule? {
        return noClassesShould()!!.callMethod("reactor.test.StepVerifier", "setDefaultTimeout", "java.time.Duration")
            .because(shouldUse("expectComplete().verify(Duration)"))
    }

    private fun noClassesShouldCallCollectorsToList(): ArchRule? {
        return noClassesShould()!!.callMethod(Collectors::class.java, "toList")
            .because(shouldUse("java.util.stream.Stream.toList()"))
    }

    private fun noClassesShouldCallURLEncoderWithStringEncoding(): ArchRule? {
        return noClassesShould()!!.callMethod(URLEncoder::class.java, "encode", String::class.java, String::class.java)
            .because(shouldUse("java.net.URLEncoder.encode(String s, Charset charset)"))
    }

    private fun noClassesShouldCallURLDecoderWithStringEncoding(): ArchRule? {
        return noClassesShould()!!.callMethod(URLDecoder::class.java, "decode", String::class.java, String::class.java)
            .because(shouldUse("java.net.URLDecoder.decode(String s, Charset charset)"))
    }

    private fun noClassesShouldLoadResourcesUsingResourceUtils(): ArchRule? {
        val resourceUtilsGetURL = hasJavaCallTarget(ownedByResourceUtils())
            .and(hasJavaCallTarget(hasNameOf("getURL")))
            .and(hasJavaCallTarget(hasRawStringParameterType()))
        val resourceUtilsGetFile = hasJavaCallTarget(ownedByResourceUtils())
            .and(hasJavaCallTarget(hasNameOf("getFile")))
            .and(hasJavaCallTarget(hasRawStringParameterType()))
        return noClassesShould()!!.callMethodWhere(resourceUtilsGetURL.or(resourceUtilsGetFile))
            .because(shouldUse("org.springframework.boot.io.ApplicationResourceLoader"))
    }

    private fun noClassesShouldCallStringToUpperCaseWithoutLocale(): ArchRule? {
        return noClassesShould()!!.callMethod(String::class.java, "toUpperCase")
            .because(shouldUse("String.toUpperCase(Locale.ROOT)"))
    }

    private fun noClassesShouldCallStringToLowerCaseWithoutLocale(): ArchRule? {
        return noClassesShould()!!.callMethod(String::class.java, "toLowerCase")
            .because(shouldUse("String.toLowerCase(Locale.ROOT)"))
    }

    private fun conditionalOnMissingBeanShouldNotSpecifyOnlyATypeThatIsTheSameAsMethodReturnType(
        annotation: String?
    ): ArchRule? {
        return methodsThatAreAnnotatedWith(annotation)!!
            .should(notSpecifyOnlyATypeThatIsTheSameAsTheMethodReturnType(annotation))
            .allowEmptyShould(true)
    }

    private fun notSpecifyOnlyATypeThatIsTheSameAsTheMethodReturnType(
        annotation: String?
    ): ArchCondition<in JavaMethod?> {
        return check<JavaMethod?>(
            "not specify only a type that is the same as the method's return type",
            BiConsumer { item: JavaMethod?, events: ConditionEvents? ->
                val conditionalAnnotation = item!!.getAnnotationOfType(annotation)
                val properties = conditionalAnnotation.properties
                if (!hasProperty("type", properties) && !hasProperty("name", properties)) {
                    conditionalAnnotation.get("value").ifPresent(Consumer { value: Any? ->
                        if (containsOnlySingleType(value as Array<JavaType?>, item.getReturnType())) {
                            ArchitectureRules.addViolation(
                                events!!, item, conditionalAnnotation.description
                                        + " should not specify only a value that is the same as the method's return type"
                            )
                        }
                    })
                }
            })
    }

    private fun hasProperty(name: String?, properties: MutableMap<String?, Any?>): Boolean {
        val property = properties.get(name)
        if (property == null) {
            return false
        }
        return if (property.javaClass.isArray()) (property as Array<Any?>).size > 0 else !property.toString().isEmpty()
    }

    private fun enumSourceShouldNotHaveValueThatIsTheSameAsTypeOfMethodsFirstParameter(): ArchRule? {
        return ArchRuleDefinition.methods()
            .that()
            .areAnnotatedWith("org.junit.jupiter.params.provider.EnumSource")
            .should(notHaveValueThatIsTheSameAsTheTypeOfTheMethodsFirstParameter())
            .allowEmptyShould(true)
    }

    private fun notHaveValueThatIsTheSameAsTheTypeOfTheMethodsFirstParameter(): ArchCondition<in JavaMethod?> {
        return check<JavaMethod?>(
            "not have a value that is the same as the type of the method's first parameter",
            BiConsumer { obj: JavaMethod?, item: ConditionEvents? ->
                ArchitectureRules.notSpecifyOnlyATypeThatIsTheSameAsTheMethodParameterType(
                    item
                )
            })
    }

    private fun notSpecifyOnlyATypeThatIsTheSameAsTheMethodParameterType(
        item: JavaMethod,
        events: ConditionEvents
    ) {
        val enumSourceAnnotation = item
            .getAnnotationOfType("org.junit.jupiter.params.provider.EnumSource")
        enumSourceAnnotation.get("value").ifPresent(Consumer { value: Any? ->
            val parameterType = item.getParameterTypes().get(0)
            if (value == parameterType) {
                addViolation(
                    events, item, enumSourceAnnotation.description
                            + " should not specify a value that is the same as the type of the method's first parameter"
                )
            }
        })
    }

    private fun classLevelConfigurationPropertiesShouldNotSpecifyOnlyPrefixAttribute(
        annotationClass: String?
    ): ArchRule? {
        return ArchRuleDefinition.classes()
            .that()
            .areAnnotatedWith(annotationClass)
            .should(notSpecifyOnlyPrefixAttributeOfConfigurationProperties(annotationClass))
            .allowEmptyShould(true)
    }

    private fun methodLevelConfigurationPropertiesShouldNotSpecifyOnlyPrefixAttribute(
        annotationClass: String?
    ): ArchRule? {
        return ArchRuleDefinition.methods()
            .that()
            .areAnnotatedWith(annotationClass)
            .should(notSpecifyOnlyPrefixAttributeOfConfigurationProperties(annotationClass))
            .allowEmptyShould(true)
    }

    private fun notSpecifyOnlyPrefixAttributeOfConfigurationProperties(
        annotationClass: String?
    ): ArchCondition<in HasAnnotations<*>?> {
        return check<HasAnnotations<*>?>(
            "not specify only prefix attribute of @ConfigurationProperties",
            BiConsumer { item: HasAnnotations<*>?, events: ConditionEvents? ->
                ArchitectureRules.notSpecifyOnlyPrefixAttributeOfConfigurationProperties(
                    annotationClass,
                    item!!,
                    events!!
                )
            })
    }

    private fun notSpecifyOnlyPrefixAttributeOfConfigurationProperties(
        annotationClass: String?,
        item: HasAnnotations<*>, events: ConditionEvents
    ) {
        val configurationPropertiesAnnotation: JavaAnnotation<*> = item.getAnnotationOfType(annotationClass)
        val properties = configurationPropertiesAnnotation.properties
        if (hasProperty("prefix", properties) && !hasProperty(
                "value",
                properties
            ) && properties.get("ignoreInvalidFields") == false
            && properties.get("ignoreUnknownFields") == true
        ) {
            addViolation(
                events, item, configurationPropertiesAnnotation.description
                        + " should specify implicit 'value' attribute other than explicit 'prefix' attribute"
            )
        }
    }

    private fun conditionsShouldNotBePublic(): ArchRule? {
        val springBootCondition = "org.springframework.boot.autoconfigure.condition.SpringBootCondition"
        return ArchRuleDefinition.noClasses()
            .that()
            .areAssignableTo(springBootCondition)
            .and()
            .doNotHaveModifier(JavaModifier.ABSTRACT)
            .and()
            .areNotAnnotatedWith(Deprecated::class.java)
            .should()
            .bePublic()
            .allowEmptyShould(true)
    }

    private fun allConfigurationPropertiesBindingBeanMethodsShouldBeStatic(annotationClass: String?): ArchRule? {
        return methodsThatAreAnnotatedWith("org.springframework.context.annotation.Bean")!!.and()
            .areAnnotatedWith(annotationClass)
            .should()
            .beStatic()
            .allowEmptyShould(true)
    }

    private fun allDeprecatedConfigurationPropertiesShouldIncludeSince(annotationName: String?): ArchRule? {
        return methodsThatAreAnnotatedWith(annotationName)!!
            .should(
                check<JavaMethod?>(
                    "include a non-empty 'since' attribute",
                    BiConsumer { method: JavaMethod?, events: ConditionEvents? ->
                        val annotation = method!!.getAnnotationOfType(annotationName)
                        val properties = annotation.properties
                        val since = properties.get("since")
                        if (since !is String || since.isEmpty()) {
                            ArchitectureRules.addViolation(
                                events!!, method, annotation.description
                                        + " should include a non-empty 'since' attribute of @DeprecatedConfigurationProperty"
                            )
                        }
                    })
            )
            .allowEmptyShould(true)
    }

    private fun autoConfigurationClassesShouldBePublicAndFinal(): ArchRule? {
        return ArchRuleDefinition.classes()
            .that(areRegularAutoConfiguration())
            .should()
            .bePublic()
            .andShould()
            .haveModifier(JavaModifier.FINAL)
            .allowEmptyShould(true)
    }

    private fun testAutoConfigurationClassesShouldBePackagePrivateAndFinal(): ArchRule? {
        return ArchRuleDefinition.classes()
            .that(areTestAutoConfiguration())
            .should()
            .bePackagePrivate()
            .andShould()
            .haveModifier(JavaModifier.FINAL)
            .allowEmptyShould(true)
    }

    private fun autoConfigurationClassesShouldHaveNoPublicMembers(): ArchRule? {
        return ArchRuleDefinition.members()
            .that()
            .areDeclaredInClassesThat(areRegularAutoConfiguration())
            .and()
            .areDeclaredInClassesThat(areNotKotlinClasses())
            .and(areNotDefaultConstructors())
            .and(areNotConstants())
            .and(dontOverridePublicMethods())
            .should()
            .notBePublic()
            .allowEmptyShould(true)
    }

    fun shouldHaveNoPublicMembers(): ArchRule? {
        return ArchRuleDefinition.members()
            .that(areNotDefaultConstructors())
            .and(areNotConstants())
            .and(dontOverridePublicMethods())
            .should()
            .notBePublic()
            .allowEmptyShould(true)
    }

    fun areRegularAutoConfiguration(): DescribedPredicate<JavaClass?>? {
        return DescribedPredicate.describe<JavaClass?>(
            "are regular @AutoConfiguration",
            Predicate { javaClass: JavaClass? ->
                javaClass!!.isAnnotatedWith(AUTOCONFIGURATION_ANNOTATION)
                        && !javaClass.name.contains("TestAutoConfiguration") && !javaClass.isAnnotation()
            })
    }

    fun areNotKotlinClasses(): DescribedPredicate<JavaClass?>? {
        return DescribedPredicate.describe<JavaClass?>(
            "are not Kotlin classes",
            Predicate { javaClass: JavaClass? -> !javaClass!!.isAnnotatedWith("kotlin.Metadata") })
    }

    fun areTestAutoConfiguration(): DescribedPredicate<JavaClass?>? {
        return DescribedPredicate.describe<JavaClass?>(
            "are test @AutoConfiguration",
            Predicate { javaClass: JavaClass? ->
                javaClass!!.isAnnotatedWith(AUTOCONFIGURATION_ANNOTATION)
                        && javaClass.name.contains("TestAutoConfiguration") && !javaClass.isAnnotation()
            })
    }

    private fun dontOverridePublicMethods(): DescribedPredicate<in JavaMember?>? {
        val predicate = OverridesPublicMethod<JavaMember?>()
        return DescribedPredicate.describe<JavaMember?>(
            "don't override public methods",
            Predicate { member: JavaMember? -> !predicate.test(member) })
    }

    private fun areNotDefaultConstructors(): DescribedPredicate<JavaMember?>? {
        return DescribedPredicate.describe<JavaMember?>(
            "aren't default constructors",
            Predicate { member: JavaMember? -> !areDefaultConstructors()!!.test(member) })
    }

    private fun areDefaultConstructors(): DescribedPredicate<JavaMember?>? {
        return DescribedPredicate.describe<JavaMember?>("are default constructors", Predicate { member: JavaMember? ->
            if (member !is JavaConstructor) {
                return@describe false
            }
            member.parameters.isEmpty()
        })
    }

    private fun areNotConstants(): DescribedPredicate<JavaMember?>? {
        return DescribedPredicate.describe<JavaMember?>(
            "aren't constants",
            Predicate { member: JavaMember? -> !areConstants()!!.test(member) })
    }

    private fun areConstants(): DescribedPredicate<JavaMember?>? {
        return DescribedPredicate.describe<JavaMember?>("are constants", Predicate { member: JavaMember? ->
            if (member is JavaField) {
                val modifiers = member.getModifiers()
                return@describe modifiers.contains(JavaModifier.STATIC) && modifiers.contains(JavaModifier.FINAL)
            }
            false
        })
    }

    private fun containsOnlySingleType(types: Array<JavaType?>, type: JavaType): Boolean {
        return types.size == 1 && type == types[0]
    }

    private fun noClassesShould(): ClassesShould? {
        return ArchRuleDefinition.noClasses().should()
    }

    private fun methodsThatAreAnnotatedWith(annotation: String?): GivenMethodsConjunction? {
        return ArchRuleDefinition.methods().that().areAnnotatedWith(annotation)
    }

    private fun ownedByResourceUtils(): DescribedPredicate<HasOwner<JavaClass?>?> {
        return HasOwner.Predicates.With.owner<JavaClass?>(JavaClass.Predicates.type(ResourceUtils::class.java))
    }

    private fun hasNameOf(name: String?): DescribedPredicate<in CodeUnitCallTarget?> {
        return HasName.Predicates.name(name)
    }

    private fun hasRawStringParameterType(): DescribedPredicate<HasParameterTypes?> {
        return HasParameterTypes.Predicates.rawParameterTypes(String::class.java)
    }

    private fun hasJavaCallTarget(
        predicate: DescribedPredicate<in CodeUnitCallTarget?>
    ): DescribedPredicate<JavaCall<*>?> {
        return JavaCall.Predicates.target(predicate)
    }

    private fun notAssignableTo(vararg typeNames: String): DescribedPredicate<JavaClass?> {
        return DescribedPredicate.not<JavaClass?>(assignableTo(*typeNames))
    }

    private fun assignableTo(vararg typeNames: String): DescribedPredicate<JavaClass?>? {
        var result: DescribedPredicate<JavaClass?>? = null
        for (typeName in typeNames) {
            val assignableTo = JavaClass.Predicates.assignableTo(typeName)
            result = if (result != null) result.or(assignableTo) else assignableTo
        }
        return result
    }

    private fun `is`(description: String, predicate: Predicate<JavaClass?>): DescribedPredicate<JavaClass?> {
        return object : DescribedPredicate<JavaClass?>(description) {
            override fun test(t: JavaClass?): Boolean {
                return predicate.test(t)
            }
        }
    }

    private fun <T> check(description: String, check: BiConsumer<T?, ConditionEvents?>): ArchCondition {
        return object : ArchCondition(description) {
            override fun check(item: T?, events: ConditionEvents?) {
                check.accept(item, events)
            }
        }
    }

    private fun addViolation(events: ConditionEvents, correspondingObject: Any?, message: String?) {
        events.add(SimpleConditionEvent.violated(correspondingObject, message))
    }

    private fun shouldUse(string: String?): String {
        return string + " should be used instead"
    }

    fun packages(filter: Predicate<JavaPackage?>?): ClassesTransformer<JavaPackage?> {
        return object : AbstractClassesTransformer<JavaPackage?>("packages") {
            override fun doTransform(collection: JavaClasses): Iterable<JavaPackage?> {
                return collection.stream().map<JavaPackage> { obj: JavaClass? -> obj!!.getPackage() }.filter(filter)
                    .collect(
                        Collectors.toSet()
                    )
            }
        }
    }

    private class OverridesPublicMethod<T : JavaMember?> : DescribedPredicate("overrides public method") {
        override fun test(member: T?): Boolean {
            if (member !is JavaMethod) {
                return false
            }
            val superClassMethods = member.getOwner()
                .getAllRawSuperclasses()
                .stream()
                .flatMap<JavaMethod> { superClass: JavaClass? -> superClass!!.getMethods().stream() }
            val interfaceMethods = member.getOwner()
                .getAllRawInterfaces()
                .stream()
                .flatMap<JavaMethod> { iface: JavaClass? -> iface!!.getMethods().stream() }
            return Stream.concat<JavaMethod?>(superClassMethods, interfaceMethods)
                .anyMatch { superMethod: JavaMethod? -> isPublic(superMethod!!) && isOverridden(superMethod, member) }
        }

        fun isPublic(method: JavaMethod): Boolean {
            return method.getModifiers().contains(JavaModifier.PUBLIC)
        }

        fun isOverridden(superMethod: JavaMethod, method: JavaMethod): Boolean {
            return superMethod.name == method.name
                    && superMethod.getRawParameterTypes().size == method.getRawParameterTypes().size && superMethod.getDescriptor() == method.getDescriptor()
        }
    }
}
