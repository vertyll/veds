rootProject.name = "project-a-microservices"

includeBuild("shared-infrastructure")
includeBuild("api-gateway")
includeBuild("auth-service")
includeBuild("mail-service")
includeBuild("role-service")
includeBuild("user-service")
