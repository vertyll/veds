pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "veds"

includeBuild("shared-saga-api")
includeBuild("shared-saga-engine")
includeBuild("shared-web")
includeBuild("shared-archunit")
includeBuild("shared-authz")
includeBuild("shared-authz-client")
includeBuild("shared-error")
includeBuild("shared-translation")
includeBuild("shared-translation-client")
includeBuild("shared-messaging-kafka")
includeBuild("iam-contracts")
includeBuild("mail-contracts")
includeBuild("project-contracts")
includeBuild("task-contracts")
includeBuild("notification-contracts")
includeBuild("template-contracts")
includeBuild("file-contracts")
includeBuild("api-gateway")
includeBuild("iam-service")
includeBuild("mail-service")
includeBuild("project-service")
includeBuild("task-service")
includeBuild("notification-service")
includeBuild("translation-service")
includeBuild("file-service")
includeBuild("template-service")
