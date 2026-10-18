rootProject.name = "notification-service"

include(":notification-domain", ":notification-application", ":notification-infrastructure")

includeBuild("../shared-error")
includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-messaging-kafka")
includeBuild("../notification-contracts")
includeBuild("../project-contracts")
includeBuild("../task-contracts")
includeBuild("../mail-contracts")
includeBuild("../iam-contracts")
