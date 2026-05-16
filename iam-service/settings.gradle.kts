rootProject.name = "iam-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "iam-domain"
project(":application").name = "iam-application"
project(":infrastructure").name = "iam-infrastructure"

includeBuild("../shared-infrastructure")
