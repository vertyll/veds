import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "IAM Microservice"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven { url = uri("https://packages.confluent.io/maven/") }
    }
}

subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)

    plugins.withId(rootProject.libs.plugins.kotlin.jvm.get().pluginId) {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
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

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn("detekt")
    }
}
