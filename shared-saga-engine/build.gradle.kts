import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    jacoco
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "Saga orchestration engine, compensation and the JPA flavour of its ports"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
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

// This creates a JAR without a main class (library)
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    // The vocabulary this engine drives. `api`, because every service that runs a
    // saga declares its steps in terms of these types.
    api("com.vertyll.veds:shared-saga-api")

    // Compensation is published through the transactional outbox rather than
    // straight to Kafka, so the engine drives the outbox processor.
    implementation("com.vertyll.veds:shared-messaging-kafka")

    // --- Common ---
    implementation(libs.bundles.spring.boot.common)

    // --- Exposed: services extend the base entities and repositories ---
    api(libs.bundles.saga.engine.api)

    // --- Annotation Processors ---
    kapt(libs.spring.boot.configuration.processor)

    // --- Testing ---
    testImplementation(libs.bundles.test.common)
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

// --- Dokka (KDoc -> HTML API docs) ---
dokka {
    moduleName.set("shared-saga-engine")
    dokkaPublications.named("html") {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("../docs/dokka/shared-saga-engine"))
    }
    dokkaSourceSets.named("main") {
        jdkVersion.set(25)
        reportUndocumented.set(false)
        skipDeprecated.set(false)
        suppressGeneratedFiles.set(true)
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/vertyll/veds/tree/main/shared-saga-engine/src/main/kotlin")
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
