import dev.detekt.gradle.Detekt

plugins {
    jacoco
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.sonarqube)
}

val kotlinVersion = libs.versions.kotlin.get()

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "Executable architecture rules every service is checked against"

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

dependencies {
    // `api`, not `implementation`: services extend the base test class and see ArchUnit's
    // types, so those have to stay on their test compile classpath.
    api(libs.archunit.junit5)
    api(libs.junit.jupiter.api)
    implementation(libs.kotlin.stdlib.jdk8)
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
        property("sonar.projectKey", "veds-shared-archunit")
        property("sonar.projectName", "veds shared-archunit")
    }
}
