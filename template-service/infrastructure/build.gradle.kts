plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom(
            libs.spring.boot.dependencies
                .get()
                .toString(),
        )
        mavenBom(
            libs.spring.cloud.dependencies
                .get()
                .toString(),
        )
        mavenBom(
            libs.testcontainers.bom
                .get()
                .toString(),
        )
    }
}

dependencies {
    implementation(project(":template-application"))
    implementation(project(":template-domain"))
    implementation("com.vertyll.veds:shared-infrastructure")
    implementation("com.vertyll.veds:template-contracts")

    implementation(libs.bundles.spring.boot.common)
    implementation(libs.bundles.spring.boot.webmvc.security)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.boot.starter.flyway) {
        exclude(group = "org.flywaydb", module = "flyway-core")
    }
    implementation(libs.bundles.flyway)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.bundles.test.common)
    testImplementation(libs.bundles.test.security)
}
