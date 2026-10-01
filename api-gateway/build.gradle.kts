import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    jacoco
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.sonarqube)
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "API Gateway Microservice"

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
    implementation("com.vertyll.veds:shared-web")

    implementation(libs.bundles.spring.boot.common)

    implementation(libs.bundles.spring.boot.gateway)
    implementation(libs.bundles.gateway.kotlin)

    implementation(libs.springdoc.openapi.starter.webflux.ui)

    if (System.getProperty("os.name").startsWith("Mac")) {
        val arch = if (System.getProperty("os.arch") == "aarch64") "osx-aarch_64" else "osx-x86_64"
        runtimeOnly(variantOf(libs.netty.resolver.dns.native.macos) { classifier(arch) })
    }

    testImplementation(libs.bundles.test.common)
    testImplementation(libs.bundles.test.gateway)
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
        property("sonar.projectKey", "veds-api-gateway")
        property("sonar.projectName", "veds api-gateway")
        property("sonar.issue.ignore.multicriteria", "tests")
        property("sonar.issue.ignore.multicriteria.tests.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.tests.resourceKey", "**/src/test/**/*.kt")
    }
}
