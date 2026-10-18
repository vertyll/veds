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
            libs.testcontainers.bom
                .get()
                .toString(),
        )
    }
}

dependencies {
    implementation(project(":translation-application"))
    implementation(project(":translation-domain"))
    implementation("com.vertyll.veds:shared-web")
    implementation("com.vertyll.veds:shared-authz")
    implementation("com.vertyll.veds:shared-authz-client")
    implementation("com.vertyll.veds:shared-messaging-kafka")
    implementation("com.vertyll.veds:iam-contracts")
    implementation("com.vertyll.veds:shared-translation")
    implementation(libs.poi.ooxml)

    implementation(libs.bundles.spring.boot.common)
    implementation(libs.bundles.spring.boot.webmvc.security)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.boot.starter.flyway) {
        exclude(group = "org.flywaydb", module = "flyway-core")
    }
    implementation(libs.bundles.flyway)

    runtimeOnly(libs.postgresql)

    testImplementation("com.vertyll.veds:shared-archunit")

    testImplementation(libs.bundles.test.common)
}
