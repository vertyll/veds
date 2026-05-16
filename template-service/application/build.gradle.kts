plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dependency.management)
}
extra["kotlin.version"] = "2.3.21"

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
    implementation(project(":domain"))
    implementation("com.vertyll.veds:shared-infrastructure")
    implementation(libs.bundles.spring.boot.common)
}
