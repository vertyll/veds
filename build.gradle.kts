plugins {
    base
}

val ktlintCheck by tasks.registering {
    group = "verification"
    description = "Runs ktlintCheck on all included builds"
    gradle.includedBuilds.forEach { build ->
        dependsOn(build.task(":ktlintCheck"))
    }
}

val ktlintFormat by tasks.registering {
    group = "formatting"
    description = "Runs ktlintFormat on all included builds"
    gradle.includedBuilds.forEach { build ->
        dependsOn(build.task(":ktlintFormat"))
    }
}

tasks.named("check") {
    dependsOn(ktlintCheck)
    gradle.includedBuilds.forEach { build ->
        dependsOn(build.task(":detekt"))
        dependsOn(build.task(":test"))
    }
}
