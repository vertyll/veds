plugins {
    base
}

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

fun aggregator(name: String, taskGroup: String, desc: String, dependsOnTask: String = name) {
    tasks.register(name) {
        group = taskGroup
        description = desc
        gradle.includedBuilds.forEach { dependsOn(it.task(":$dependsOnTask")) }
    }
}

aggregator("ktlintCheck",  "verification", "Runs ktlintCheck on all included builds")
aggregator("ktlintFormat", "formatting",   "Runs ktlintFormat on all included builds")
aggregator("detekt",       "verification", "Runs detekt on all included builds")
aggregator("test",         "verification", "Runs all tests across all included builds")

tasks.named("build") {
    gradle.includedBuilds.forEach { dependsOn(it.task(":build")) }
}

tasks.named("clean") {
    gradle.includedBuilds.forEach { dependsOn(it.task(":clean")) }
}

tasks.named("check") {
    dependsOn("ktlintCheck", "detekt", "test")
}

tasks.register("docs") {
    group = "documentation"
    description = "Generates Dokka HTML docs for shared-infrastructure (output: shared-infrastructure/build/dokka/html)"
    dependsOn(gradle.includedBuild("shared-infrastructure").task(":dokkaGenerate"))
}

val composeCli = listOf("podman", "compose")
val infraServices = listOf(
    "kafka", "schema-registry",
    "iam-db", "mail-db", "template-db", "keycloak-db",
    "keycloak", "mail-dev", "kafka-ui",
)

tasks.register<Exec>("infraUp") {
    group = "infra"
    description = "Starts the local infrastructure stack (Kafka, Schema Registry, Postgres, Keycloak, …)"
    workingDir = projectDir
    commandLine(composeCli + listOf("up", "-d") + infraServices)
}

tasks.register<Exec>("infraDown") {
    group = "infra"
    description = "Stops and removes the local infrastructure stack"
    workingDir = projectDir
    commandLine(composeCli + listOf("down"))
}

tasks.register<Exec>("infraLogs") {
    group = "infra"
    description = "Tails logs from all infrastructure containers (Ctrl-C to exit)"
    workingDir = projectDir
    commandLine(composeCli + listOf("logs", "-f", "--tail=200"))
    standardInput = System.`in`
}

tasks.register<Exec>("provisionTopics") {
    group = "infra"
    description = "Runs the Terraform topics-init container (idempotent)"
    workingDir = projectDir
    commandLine(composeCli + listOf("run", "--rm", "topics-init"))
}

tasks.register<Exec>("registerSchemas") {
    group = "infra"
    description = "Registers all Avro schemas in the Schema Registry (idempotent)"
    workingDir = projectDir
    commandLine(composeCli + listOf("run", "--rm", "schemas-init"))
}

tasks.register("bootstrap") {
    group = "infra"
    description = "Provisions Kafka topics + registers Avro schemas (run once after infraUp)"
    dependsOn("provisionTopics", "registerSchemas")
}
