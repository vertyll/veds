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
    alias(libs.plugins.sonarqube)
    `jacoco-report-aggregation`
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "IAM Microservice"

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
                languageVersion = JavaLanguageVersion.of(rootProject.libs.versions.java.get())
            }
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                freeCompilerArgs.addAll(
                    "-Xjsr305=strict",
                )
            }
        }

        tasks.withType<Test> {
            jvmArgs("-Xshare:off")
            useJUnitPlatform {
                if (!project.hasProperty("integrationTests")) {
                    excludeTags("integration")
                }
            }
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        }
    }

    configurations.matching { it.name.startsWith("ktlint") }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(kotlinVersion)
            }
        }
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(rootProject.libs.versions.ktlint.engine.get())
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

    pluginManager.apply("jacoco")

    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required = true
            html.required = true
        }
    }

    tasks.matching { it.name == "test" }.configureEach {
        finalizedBy("jacocoTestReport")
    }
}

tasks.register("checkHexagonalDependencies") {
    group = "verification"
    description = "Fails if a framework reaches the application layer"
    dependsOn(
        subprojects
            .filter { it.name.endsWith("-application") }
            .map { "${it.path}:checkHexagonalDependencies" },
    )
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

dependencies {
    jacocoAggregation(platform(libs.spring.boot.dependencies))
    subprojects.forEach { jacocoAggregation(project(it.path)) }
}

reporting {
    reports {
        register<JacocoCoverageReport>("testCodeCoverageReport") {
            testSuiteName = "test"
        }
    }
}

val aggregatedCoverage = layout.buildDirectory.file("reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")

tasks.named<JacocoReport>("testCodeCoverageReport") {
    reports {
        xml.required = true
    }
}

subprojects {
    sonar {
        properties {
            property("sonar.coverage.jacoco.xmlReportPaths", aggregatedCoverage.get().asFile.path)
        }
    }
}

tasks.named("sonar") {
    dependsOn("testCodeCoverageReport")
}

sonar {
    properties {
        property("sonar.projectKey", "veds-iam-service")
        property("sonar.projectName", "veds iam-service")
        property("sonar.issue.ignore.multicriteria", "dto,command,domainModel,beans,tests,repository,entity")
        property("sonar.issue.ignore.multicriteria.dto.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.dto.resourceKey", "**/dto/*.kt")
        property("sonar.issue.ignore.multicriteria.command.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.command.resourceKey", "**/command/*.kt")
        property("sonar.issue.ignore.multicriteria.domainModel.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.domainModel.resourceKey", "**/domain/model/*.kt")
        property("sonar.issue.ignore.multicriteria.beans.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.beans.resourceKey", "**/config/ApplicationBeansConfig.kt")
        property("sonar.issue.ignore.multicriteria.tests.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.tests.resourceKey", "**/src/test/**/*.kt")
        property("sonar.issue.ignore.multicriteria.repository.ruleKey", "kotlin:S6517")
        property("sonar.issue.ignore.multicriteria.repository.resourceKey", "**/persistence/repository/*.kt")
        property("sonar.issue.ignore.multicriteria.entity.ruleKey", "kotlin:S6524")
        property("sonar.issue.ignore.multicriteria.entity.resourceKey", "**/persistence/entity/*.kt")
        property("sonar.cpd.exclusions", "**/config/TranslationCatalogueConfig.kt")
    }
}
