pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "shared-saga-engine"

includeBuild("../shared-saga-api")
includeBuild("../shared-messaging-kafka")
