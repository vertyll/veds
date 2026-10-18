rootProject.name = "template-service"

include(":template-domain", ":template-application", ":template-infrastructure")

includeBuild("../shared-saga-api")
includeBuild("../shared-error")
includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-saga-engine")
includeBuild("../shared-messaging-kafka")
includeBuild("../template-contracts")
