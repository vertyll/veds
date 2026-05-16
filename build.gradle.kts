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
