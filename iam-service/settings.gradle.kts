rootProject.name = "iam-service"

include(":iam-domain", ":iam-application", ":iam-infrastructure")

includeBuild("../shared-error")
includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-authz")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-messaging-kafka")
includeBuild("../iam-contracts")
includeBuild("../mail-contracts")
