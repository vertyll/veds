package com.vertyll.veds.shared.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.core.importer.Location
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Base class that runs the shared architecture rules against one service.
 *
 * A service adds a single subclass naming its base package:
 * ```kotlin
 * class ProjectArchitectureTest : VedsArchitectureTest("com.vertyll.veds.project")
 * ```
 *
 * Jars are deliberately part of the import: `domain` and `application` are separate Gradle
 * projects, so their classes reach the infrastructure test classpath packaged. Excluding jars
 * leaves those layers empty, and the rules then pass or fail for the wrong reason.
 */

abstract class VedsArchitectureTest(
    private val basePackage: String,
) {
    private val classes: JavaClasses by lazy {
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(NoKotlinCompilerArtefacts)
            .importPackages(basePackage)
    }

    @Test
    @DisplayName("the dependency rule points inwards")
    fun dependencyRulePointsInwards() = VedsArchitectureRules.layering(basePackage).check(classes)

    @Test
    @DisplayName("the domain layer is framework-free")
    fun domainLayerIsFrameworkFree() = VedsArchitectureRules.domainIsFrameworkFree(basePackage).check(classes)

    @Test
    @DisplayName("the application layer is framework-free")
    fun applicationLayerIsFrameworkFree() = VedsArchitectureRules.applicationIsFrameworkFree(basePackage).check(classes)

    @Test
    @DisplayName("JPA entities live in the persistence adapter")
    fun jpaEntitiesLiveInPersistenceAdapter() = VedsArchitectureRules.jpaEntitiesLiveInPersistence(basePackage).check(classes)

    @Test
    @DisplayName("controllers live in the web adapter")
    fun controllersLiveInWebAdapter() = VedsArchitectureRules.controllersLiveInWebAdapter(basePackage).check(classes)

    @Test
    @DisplayName("every endpoint declares where its authorization decision is taken")
    fun everyEndpointDeclaresItsAuthorization() = VedsArchitectureRules.everyEndpointDeclaresItsAuthorization().check(classes)

    @Test
    @DisplayName("only infrastructure talks to Kafka")
    fun onlyInfrastructureTalksToKafka() = VedsArchitectureRules.kafkaOnlyInInfrastructure(basePackage).check(classes)

    @Test
    @DisplayName("application ports are interfaces")
    fun applicationPortsAreInterfaces() = VedsArchitectureRules.portsAreInterfaces(basePackage).check(classes)

    @Test
    @DisplayName("repository ports are interfaces")
    fun repositoryPortsAreInterfaces() = VedsArchitectureRules.repositoryPortsAreInterfaces(basePackage).check(classes)

    @Test
    @DisplayName("adapters live in infrastructure")
    fun adaptersLiveInInfrastructure() = VedsArchitectureRules.adaptersLiveInInfrastructure(basePackage).check(classes)

    @Test
    @DisplayName("nothing writes to standard streams")
    fun nothingWritesToStandardStreams() = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(classes)

    @Test
    @DisplayName("nothing throws generic exceptions")
    fun nothingThrowsGenericExceptions() = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(classes)

    @Test
    @DisplayName("nothing uses java util logging")
    fun nothingUsesJavaUtilLogging() = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(classes)

    @Test
    @DisplayName("nothing uses field injection")
    fun nothingUsesFieldInjection() = NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(classes)
}

/**
 * Drops classes the Kotlin compiler synthesises rather than a developer writing them:
 * `DefaultImpls` (bodies of interface default methods) and `WhenMappings` (enum `when` tables).
 * Judging them reports compiler strategy, not architecture — `DefaultImpls` is a class, so an
 * "interfaces only" rule fails on every interface that has a default method.
 */
private val NoKotlinCompilerArtefacts =
    ImportOption { location: Location ->
        !location.contains($$"$DefaultImpls") && !location.contains($$"$WhenMappings")
    }
