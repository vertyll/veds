rootProject.name = "iam-service"

include(":domain", ":application", ":infrastructure")

includeBuild("../shared-infrastructure")
