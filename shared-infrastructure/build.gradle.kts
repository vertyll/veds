import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
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
description = "Shared infrastructure for microservices"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.eclipse.jetty:jetty-bom:9.4.59"))
            .using(module("org.eclipse.jetty:jetty-bom:9.4.59.v20231031"))
            .because("Confluent 7.8.7 references jetty-bom:9.4.59 which is not published")
    }
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
            libs.testcontainers.bom
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
    // --- Common ---
    implementation(libs.bundles.spring.boot.common)

    // --- Infrastructure API ---
    api(libs.bundles.shared.infrastructure.api)
    api(libs.apache.avro)
    api(libs.confluent.kafka.avro.serializer)

    // --- Reactor (needed for ReactiveKeycloakJwtAuthenticationConverter, provided at runtime by gateway) ---
    compileOnly("io.projectreactor:reactor-core")

    // --- Annotation Processors ---
    kapt(libs.spring.boot.configuration.processor)

    // --- Testing ---
    testImplementation(libs.bundles.test.common)
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

// --- Dokka (KDoc -> HTML API docs) ---
dokka {
    moduleName.set("shared-infrastructure")
    dokkaPublications.named("html") {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("../docs/dokka"))
    }
    dokkaSourceSets.named("main") {
        jdkVersion.set(25)
        reportUndocumented.set(false)
        skipDeprecated.set(false)
        suppressGeneratedFiles.set(true)
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/vertyll/veds/tree/main/shared-infrastructure/src/main/kotlin")
            remoteLineSuffix.set("#L")
        }
    }
}
