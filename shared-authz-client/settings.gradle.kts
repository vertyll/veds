pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "shared-authz-client"

includeBuild("../shared-authz")
