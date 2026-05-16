rootProject.name = "mail-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "mail-domain"
project(":application").name = "mail-application"
project(":infrastructure").name = "mail-infrastructure"

includeBuild("../shared-infrastructure")
includeBuild("../iam-contracts")
includeBuild("../mail-contracts")
