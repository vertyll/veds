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

// --- Avro code generation (SpecificRecord) ---
// NOTE: template-service is a *template* for cloning new microservices, so its
// Avro schemas live LOCALLY (src/main/avro) and are intentionally NOT placed
// under the shared `contracts/` directory. That keeps them out of the global
// schema-registry registration and Terraform topic provisioning.
val avroTools: Configuration by configurations.creating
val avroContractsDir = file("$projectDir/src/main/avro")
val avroGeneratedDir = layout.buildDirectory.dir("generated/sources/avro/main/java")
val avroSchemas = fileTree(avroContractsDir) { include("**/*.avsc") }

val generateAvroJava by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Generate Java SpecificRecord classes from all Avro schemas in contracts/."
    inputs
        .files(avroSchemas)
        .withPropertyName("avroSchemas")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(avroGeneratedDir).withPropertyName("avroGeneratedDir")
    classpath = avroTools
    mainClass = "org.apache.avro.tool.Main"

    doFirst {
        val outDir = avroGeneratedDir.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()
    }
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf("compile", "schema", "-string") +
                avroSchemas.files.map { it.absolutePath } +
                listOf(avroGeneratedDir.get().asFile.absolutePath)
        },
    )
}

sourceSets {
    main {
        java.srcDir(generateAvroJava)
    }
}

dependencies {
    avroTools(libs.apache.avro.tools)

    implementation(project(":template-application"))
    implementation(project(":template-domain"))
    implementation("com.vertyll.veds:shared-infrastructure")

    implementation(libs.bundles.spring.boot.common)
    implementation(libs.bundles.spring.boot.webmvc.security)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.boot.starter.flyway) {
        exclude(group = "org.flywaydb", module = "flyway-core")
    }
    implementation(libs.bundles.flyway)
    runtimeOnly(libs.postgresql)
    developmentOnly(libs.spring.boot.devtools)

    testImplementation(libs.bundles.test.common)
    testImplementation(libs.bundles.test.security)
}
