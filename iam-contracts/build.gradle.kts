// =========================================================================
// iam-contracts — Published Language module (DDD).
// =========================================================================
plugins {
    java
    `java-library`
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "IAM bounded-context Avro contracts"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

repositories {
    mavenCentral()
}

configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

val avroVersion = "1.12.1"

val avroTools: Configuration by configurations.creating
val avroContractsDir = file("$rootDir/../contracts/iam-service")
val avroGeneratedDir = layout.buildDirectory.dir("generated/sources/avro/main/java")
val avroSchemas = fileTree(avroContractsDir) { include("**/*.avsc") }

val generateAvroJava by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Generate Java SpecificRecord classes from all Avro schemas owned by the IAM bounded context."
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
    avroTools("org.apache.avro:avro-tools:$avroVersion") {
        exclude(group = "org.apache.avro", module = "trevni-avro")
        exclude(group = "org.apache.avro", module = "trevni-core")
    }

    api("org.apache.avro:avro:$avroVersion")
}

tasks.jar {
    enabled = true
}
