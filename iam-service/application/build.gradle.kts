plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom(
            libs.spring.boot.dependencies
                .get()
                .toString(),
        )
    }
}

dependencies {
    implementation(project(":iam-domain"))
    implementation("com.vertyll.veds:shared-infrastructure")

    implementation(libs.bundles.spring.boot.common)
    implementation(libs.spring.boot.starter.validation)
}
