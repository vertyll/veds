rootProject.name = "file-service"

include(":file-domain", ":file-application", ":file-infrastructure")

includeBuild("../shared-error")
includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-messaging-kafka")
includeBuild("../file-contracts")
