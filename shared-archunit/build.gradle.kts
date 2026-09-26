import dev.detekt.gradle.Detekt

plugins {
    jacoco
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

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

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
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
