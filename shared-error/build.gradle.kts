import dev.detekt.gradle.Detekt

plugins {
    jacoco
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.sonarqube)
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "Declaration of the permissions a service enforces, and the role projection it authorizes from"

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
    implementation(libs.kotlin.stdlib.jdk8)

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
    moduleName.set("shared-error")
    dokkaPublications.named("html") {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("../docs/dokka/shared-error"))
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

tasks.named("sonar") {
    dependsOn("jacocoTestReport")
}

sonar {
    properties {
        property("sonar.projectKey", "veds-shared-error")
        property("sonar.projectName", "veds shared-error")
        property("sonar.issue.ignore.multicriteria", "tests")
        property("sonar.issue.ignore.multicriteria.tests.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.tests.resourceKey", "**/src/test/**/*.kt")
    }
}
