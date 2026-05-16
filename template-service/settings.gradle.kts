rootProject.name = "template-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "template-domain"
project(":application").name = "template-application"
project(":infrastructure").name = "template-infrastructure"

includeBuild("../shared-infrastructure")
