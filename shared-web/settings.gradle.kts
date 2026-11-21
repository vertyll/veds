pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "shared-web"
includeBuild("../shared-authz")
includeBuild("../shared-error")
