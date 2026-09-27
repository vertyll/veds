plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":template-domain"))
    implementation("com.vertyll.veds:shared-error")
    implementation("com.vertyll.veds:shared-saga-api")

    testImplementation(libs.bundles.test.unit)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/** Fails the build if a framework reaches the application layer. */
val forbiddenOnApplicationClasspath =
    listOf(
        "org.springframework",
        "jakarta.validation",
        "jakarta.persistence",
        "tools.jackson",
        "com.fasterxml.jackson",
        "org.slf4j",
        "org.apache.avro",
        "org.apache.kafka",
        "org.hibernate",
    )

val checkHexagonalDependencies =
    tasks.register("checkHexagonalDependencies") {
        group = "verification"
        description = "Asserts that no framework is on the application layer's compile classpath."

        val classpath = configurations.named("compileClasspath")

        doLast {
            val offenders =
                classpath
                    .get()
                    .resolvedConfiguration
                    .resolvedArtifacts
                    .asSequence()
                    .map { it.moduleVersion.id }
                    .filter { id -> forbiddenOnApplicationClasspath.any { id.group.startsWith(it) } }
                    .map { "${it.group}:${it.name}" }
                    .distinct()
                    .sorted()
                    .toList()

            if (offenders.isNotEmpty()) {
                throw GradleException(
                    buildString {
                        appendLine("The application layer must not depend on a framework.")
                        appendLine("Found on its compile classpath:")
                        offenders.forEach { appendLine("  - $it") }
                        appendLine()
                        appendLine("Move the framework concern into `infrastructure` and express it")
                        appendLine("as a port. See docs/hexagonal-layering.md.")
                    },
                )
            }
        }
    }

tasks.named("check") {
    dependsOn(checkHexagonalDependencies)
}
