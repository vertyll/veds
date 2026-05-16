rootProject.name = "template-service"

include(":domain", ":application", ":infrastructure")

includeBuild("../shared-infrastructure")
