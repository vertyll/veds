import dev.detekt.gradle.Detekt

plugins {
    jacoco
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "Translation key declaration DSL and ICU message rendering, shared across services"

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
    // ICU4J, and nothing from Spring.
    //
    // `java.text.MessageFormat` cannot do this job: it has no CLDR plural rules,
    // so Polish `one/few/many/other` — "1 zadanie", "3 zadania", "5 zadań" —
    // is simply not expressible. ICU4J carries the CLDR data.
    //
    // This module is referenced by application layers, which must stay
    // framework-free, so it deliberately depends on nothing else.
    implementation(libs.kotlin.stdlib.jdk8)
    api(libs.icu4j)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
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

dokka {
    moduleName.set("shared-translation")
    dokkaPublications.named("html") {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("../docs/dokka/shared-translation"))
    }
    dokkaSourceSets.named("main") {
        jdkVersion.set(25)
        reportUndocumented.set(false)
        skipDeprecated.set(false)
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
