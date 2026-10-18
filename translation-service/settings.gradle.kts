rootProject.name = "translation-service"

include(":translation-domain", ":translation-application", ":translation-infrastructure")

includeBuild("../shared-error")
includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-translation")
includeBuild("../shared-authz")
includeBuild("../shared-authz-client")
includeBuild("../shared-messaging-kafka")
includeBuild("../iam-contracts")
