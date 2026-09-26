plugins {
    base
}

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

/** Every included build that carries Kotlin — the `-contracts` builds only hold Avro schemas. */
val codeBuilds = gradle.includedBuilds.filterNot { it.name.endsWith("-contracts") }

/**
 * The hexagonal services.
 *
 * Only these register `checkHexagonalDependencies`, and only these tag their integration tests
 * out of the default `test` run, so `api-gateway` and the `shared-*` libraries are excluded from
 * both aggregations — asking them for a task they never registered would just fail the build.
 */
val serviceBuilds = codeBuilds.filter { it.name.endsWith("-service") }

/**
 * Set by `./gradlew test -PintegrationTests` — see docs/testing.md.
 *
 * The flag only widens what each build runs; it does not change which builds take part. Deciding
 * that by name would go stale the moment a library grows an integration test of its own, and a
 * test nobody runs is worse than no test.
 */
val integrationTests = hasProperty("integrationTests")

fun aggregator(
    name: String,
    taskGroup: String,
    desc: String,
    builds: List<IncludedBuild> = codeBuilds,
    dependsOnTask: String = name,
) {
    tasks.register(name) {
        group = taskGroup
        description = desc
        builds.forEach { dependsOn(it.task(":$dependsOnTask")) }
    }
}

aggregator("ktlintCheck", "verification", "Runs ktlintCheck on all included builds")
aggregator("ktlintFormat", "formatting", "Runs ktlintFormat on all included builds")
aggregator("detekt", "verification", "Runs detekt on all included builds")

aggregator(
    "checkHexagonalDependencies",
    "verification",
    "Fails if a framework reaches the application layer of any service",
    builds = serviceBuilds,
)

aggregator(
    "test",
    "verification",
    if (integrationTests) {
        "Runs unit and integration tests across all included builds (-PintegrationTests)"
    } else {
        "Runs all tests across all included builds"
    },
)

tasks.named("build") {
    gradle.includedBuilds.forEach { dependsOn(it.task(":build")) }
}

tasks.named("clean") {
    gradle.includedBuilds.forEach { dependsOn(it.task(":clean")) }
}

tasks.named("check") {
    dependsOn("ktlintCheck", "detekt", "checkHexagonalDependencies", "test")
}

/**
 * Every shared library that publishes KDoc: its name, the blurb shown on the landing page, and
 * whether it is safe for an application layer to depend on. A module joins the published
 * documentation by being added here.
 */
data class DocumentedLibrary(
    val name: String,
    val blurb: String,
    val frameworkFree: Boolean,
)

val documentedLibraries =
    listOf(
        DocumentedLibrary("shared-saga-api", "The saga vocabulary and the port an application service drives it through", true),
        DocumentedLibrary("shared-translation", "Translation key DSL and the ICU message renderer", true),
        DocumentedLibrary("shared-authz", "Permission catalogue DSL, role scopes and the projection port", true),
        DocumentedLibrary("shared-error", "The error contract every service's catalogue implements", true),
        DocumentedLibrary("shared-web", "Keycloak authentication, HTTP concurrency helpers and shared configuration", false),
        DocumentedLibrary("shared-messaging-kafka", "Transactional outbox, idempotent consumption and Avro over Kafka", false),
        DocumentedLibrary("shared-saga-engine", "One service's local saga: state machine, compensation and the JPA flavour of its ports", false),
        DocumentedLibrary("shared-translation-client", "Start-up registration of a service's translation keys", false),
        DocumentedLibrary("shared-authz-client", "Start-up registration of a module's permission catalogue with iam-service", false),
    )

tasks.register("docs") {
    group = "documentation"
    description = "Generates Dokka HTML docs for the shared libraries (output: docs/dokka/index.html)"

    documentedLibraries.forEach { dependsOn(gradle.includedBuild(it.name).task(":dokkaGenerate")) }

    val landingPage = rootDir.resolve("docs/dokka/index.html")
    val libraries = documentedLibraries

    val template = rootDir.resolve("gradle/docs/landing.html")
    inputs.file(template)

    doLast {
        // Dokka writes one self-contained site per module. Without a landing page the published
        // root would be a bare directory listing.
        fun cards(entries: List<DocumentedLibrary>) =
            entries.joinToString("\n") {
                """      <li><a href="${it.name}/index.html"><code>${it.name}</code></a><span>${it.blurb}</span></li>"""
            }

        landingPage.parentFile.mkdirs()
        landingPage.writeText(
            template
                .readText()
                .replace("@@PROJECT@@", "veds")
                .replace("@@SUBJECT@@", "shared library")
                .replace("@@HEADING@@", "shared libraries")
                .replace("@@LEDE@@", "API documentation generated from KDoc with Dokka.")
                .replace("@@PURE_SCOPE@@", "safe for an application layer")
                .replace("@@UNIT@@", "module")
                .replace("@@PURE@@", cards(libraries.filter { it.frameworkFree }))
                .replace("@@SPRING@@", cards(libraries.filterNot { it.frameworkFree })),
        )
        logger.lifecycle("Dokka HTML docs: ${landingPage.toURI()}")
    }
}
