plugins {
    alias(libs.plugins.kotlin.jvm)
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
    implementation(libs.kotlin.stdlib.jdk8)
    api("org.springframework.data:spring-data-commons")
}
