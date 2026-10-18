rootProject.name = "task-service"

include(":task-domain", ":task-application", ":task-infrastructure")

includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-error")
includeBuild("../shared-authz")
includeBuild("../shared-authz-client")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-messaging-kafka")
includeBuild("../task-contracts")
includeBuild("../project-contracts")
includeBuild("../iam-contracts")
includeBuild("../file-contracts")
