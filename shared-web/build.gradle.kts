import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    jacoco
    alias(libs.plugins.sonarqube)
}

val kotlinVersion =
    libs.versions.kotlin
        .asProvider()
        .get()

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "Keycloak authentication, HTTP concurrency helpers and shared configuration"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

repositories {
    mavenCentral()
}

configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

dependencyManagement {
    imports {
        mavenBom(
            libs.spring.boot.dependencies
                .get()
                .toString(),
        )
    }
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.kotlin.logging)

    // Security, part of this module's public surface
    api("com.vertyll.veds:shared-authz")
    api("com.vertyll.veds:shared-error")
    api(libs.bundles.web.api)
    api(libs.springframework.tx)

    // OpenAPI: only contributes a bean when the service already brings springdoc
    compileOnly(libs.swagger.core.models)

    // Serializing a problem document needs the container's mapper, which the service brings
    compileOnly("io.projectreactor:reactor-core")
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jakarta.servlet.api)
    testImplementation(libs.swagger.core.models)
    testImplementation(libs.jackson.databind)

    // Reactor: only ReactiveKeycloakJwtAuthenticationConverter needs it
    testImplementation("io.projectreactor:reactor-core")

    kapt(libs.spring.boot.configuration.processor)

    testImplementation(libs.bundles.test.common)
}

configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
        }
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(
        libs.versions.ktlint.engine
            .get(),
    )
    debug.set(false)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude { element -> element.file.path.contains("generated/") }
        include("**/src/**/*.kt")
        include("**/src/**/*.kts")
    }
}

tasks.withType<Detekt>().configureEach {
    config.setFrom(files("${rootProject.projectDir}/../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.named("check") {
    dependsOn("detekt")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
        )
    }
}

tasks.withType<Test> {
    jvmArgs("-Xshare:off")
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

dokka {
    moduleName.set("shared-web")
    dokkaPublications.named("html") {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("../docs/dokka/shared-web"))
    }
    dokkaSourceSets.named("main") {
        jdkVersion.set(25)
        reportUndocumented.set(false)
        skipDeprecated.set(false)
        suppressGeneratedFiles.set(true)
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/vertyll/veds/tree/main/shared-web/src/main/kotlin")
            remoteLineSuffix.set("#L")
        }
    }
}

tasks.withType<JacocoReport>().configureEach {
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.named("test") {
    finalizedBy("jacocoTestReport")
}

tasks.named("sonar") {
    dependsOn("jacocoTestReport")
}

sonar {
    properties {
        property("sonar.projectKey", "veds-shared-web")
        property("sonar.projectName", "veds shared-web")
        property("sonar.issue.ignore.multicriteria", "tests")
        property("sonar.issue.ignore.multicriteria.tests.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.tests.resourceKey", "**/src/test/**/*.kt")
    }
}
