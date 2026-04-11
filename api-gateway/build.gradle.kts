import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "API Gateway Microservice"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

repositories {
    mavenCentral()
}

configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencyManagement {
    imports {
        mavenBom(
            libs.spring.boot.dependencies
                .get()
                .toString(),
        )
        mavenBom(
            libs.spring.cloud.dependencies
                .get()
                .toString(),
        )
        mavenBom(
            libs.testcontainers.bom
                .get()
                .toString(),
        )
    }
}

dependencies {
    // --- Common ---
    implementation(libs.bundles.spring.boot.common)
    implementation("com.vertyll.veds:shared-infrastructure")

    // --- Gateway & Reactive ---
    implementation(libs.bundles.spring.boot.gateway)
    implementation(libs.bundles.gateway.kotlin)

    // --- API Documentation ---
    implementation(libs.springdoc.openapi.starter.webflux.ui)

    // --- Development Tools ---
    developmentOnly(libs.spring.boot.devtools)

    // --- Testing ---
    testImplementation(libs.bundles.test.common)
    testImplementation(libs.bundles.test.gateway)
}

// Configure ktlint
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
