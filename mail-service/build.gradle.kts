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
description = "Mail Microservice"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

val kotlinVersion = libs.versions.kotlin.asProvider().get()

allprojects {
    group = rootProject.group
    version = rootProject.version

    extra["kotlin.version"] = kotlinVersion

    repositories {
        mavenCentral()
        maven { url = uri("https://packages.confluent.io/maven/") }
    }
}

subprojects {
    pluginManager.apply(rootProject.libs.plugins.ktlint.get().pluginId)
    pluginManager.apply(rootProject.libs.plugins.detekt.get().pluginId)

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

listOf("test", "detekt", "ktlintCheck", "ktlintFormat").forEach { taskName ->
    tasks.register(taskName) {
        group = if (taskName == "ktlintFormat") "formatting" else "verification"
        description = "Aggregates :$taskName across all subprojects"
        dependsOn(subprojects.map { "${it.path}:$taskName" })
    }
}

listOf("build", "clean").forEach { taskName ->
    tasks.register(taskName) {
        group = "build"
        description = "Aggregates :$taskName across all subprojects"
        dependsOn(subprojects.map { "${it.path}:$taskName" })
    }
}
