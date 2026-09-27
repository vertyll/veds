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
description = "Framework-free saga vocabulary shared by the application layers"

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
    // Nothing but the Kotlin standard library, deliberately.
    //
    // These types are the saga vocabulary spoken by the application layer of every
    // service that owns a saga step. That layer must stay free of frameworks, so
    // this module depends on nothing that could drag one in. The Spring-bound
    // engine driving these types is `shared-saga-engine`, which depends on this
    // module and never the other way round.
    implementation(libs.kotlin.stdlib.jdk8)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
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
    moduleName.set("shared-saga-api")
    dokkaPublications.named("html") {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("../docs/dokka/shared-saga-api"))
    }
    dokkaSourceSets.named("main") {
        jdkVersion.set(25)
        reportUndocumented.set(false)
        skipDeprecated.set(false)
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/vertyll/veds/tree/main/shared-saga-api/src/main/kotlin")
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

tasks.withType<Test> {
    useJUnitPlatform()
}
