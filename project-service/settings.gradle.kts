rootProject.name = "project-service"

include(":project-domain", ":project-application", ":project-infrastructure")

includeBuild("../shared-saga-api")
includeBuild("../shared-error")
includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-authz")
includeBuild("../shared-authz-client")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-saga-engine")
includeBuild("../shared-messaging-kafka")
includeBuild("../project-contracts")
includeBuild("../mail-contracts")
includeBuild("../iam-contracts")
