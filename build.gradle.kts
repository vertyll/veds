plugins {
    base
}

val ktlintCheck by tasks.registering {
    group = "verification"
    description = "Runs ktlintCheck on all included builds"
    val builds = listOf(
        "api-gateway", "auth-service", "mail-service", "role-service",
        "shared-infrastructure", "template-service", "user-service"
    )
    builds.forEach { buildName ->
        dependsOn(gradle.includedBuild(buildName).task(":ktlintCheck"))
    }
}

val ktlintFormat by tasks.registering {
    group = "formatting"
    description = "Runs ktlintFormat on all included builds"
    val builds = listOf(
        "api-gateway", "auth-service", "mail-service", "role-service",
        "shared-infrastructure", "template-service", "user-service"
    )
    builds.forEach { buildName ->
        dependsOn(gradle.includedBuild(buildName).task(":ktlintFormat"))
    }
}

tasks.named("check") {
    dependsOn(ktlintCheck)
    val builds = listOf(
        "api-gateway", "auth-service", "mail-service", "role-service",
        "shared-infrastructure", "template-service", "user-service"
    )
    builds.forEach { buildName ->
        dependsOn(gradle.includedBuild(buildName).task(":detekt"))
        dependsOn(gradle.includedBuild(buildName).task(":test"))
    }
}
