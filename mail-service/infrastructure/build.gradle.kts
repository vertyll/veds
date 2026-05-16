plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
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
sourceSets {
    main {
        java.srcDir(layout.buildDirectory.dir("generated/sources/avro/main/java"))
    }
}

val avroTools: Configuration by configurations.creating
val avroContractsDir = file("$rootDir/../contracts")
val avroGeneratedDir = layout.buildDirectory.dir("generated/sources/avro/main/java")

val generateAvroJava by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Generate Java SpecificRecord classes from all Avro schemas in contracts/."
    inputs.dir(avroContractsDir)
    outputs.dir(avroGeneratedDir)
    classpath = avroTools
    mainClass.set("org.apache.avro.tool.Main")
    doFirst {
        val outDir = avroGeneratedDir.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()
        val schemas =
            avroContractsDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "avsc" }
                .map { it.absolutePath }
                .toList()
        args = listOf("compile", "schema", "-string") + schemas + listOf(outDir.absolutePath)
    }
}

tasks.named("compileKotlin") { dependsOn(generateAvroJava) }
tasks.named("compileJava") { dependsOn(generateAvroJava) }
tasks
    .matching { it.name.contains("Ktlint", ignoreCase = true) }
    .configureEach { dependsOn(generateAvroJava) }

dependencies {
    avroTools(libs.apache.avro.tools)

    implementation(project(":application"))
    implementation(project(":domain"))
    implementation("com.vertyll.veds:shared-infrastructure")

    implementation(libs.bundles.spring.boot.common)
    implementation(libs.bundles.spring.boot.webmvc.security)
    implementation(libs.bundles.spring.boot.mail)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.boot.starter.flyway) {
        exclude(group = "org.flywaydb", module = "flyway-core")
    }
    implementation(libs.bundles.flyway)
    runtimeOnly(libs.postgresql)
    developmentOnly(libs.spring.boot.devtools)

    testImplementation(libs.bundles.test.common)
    testImplementation(libs.bundles.test.mail)
    testImplementation(libs.springframework.kafka.test)
}
