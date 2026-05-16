rootProject.name = "mail-service"

include(":domain", ":application", ":infrastructure")

includeBuild("../shared-infrastructure")
