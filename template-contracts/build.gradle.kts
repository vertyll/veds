// =========================================================================
// template-contracts — Published Language module (DDD).
// =========================================================================
plugins {
    java
    `java-library`
}

group = "com.vertyll.veds"
version = "0.0.1-SNAPSHOT"
description = "Template bounded-context Avro contracts"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

repositories {
    mavenCentral()
}

configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

val avroTools: Configuration = configurations.create("avroTools")
val avroContractsDir = file("$projectDir/avro")
val avroGeneratedDir = layout.buildDirectory.dir("generated/sources/avro/main/java")
val avroSchemas = fileTree(avroContractsDir) { include("**/*.avsc") }

val generateAvroJava = tasks.register<JavaExec>("generateAvroJava") {
    group = "build"
    description = "Generate Java SpecificRecord classes from all Avro schemas owned by the template bounded context."
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
    avroTools(libs.avro.tools) {
        exclude(group = "org.apache.avro", module = "trevni-avro")
        exclude(group = "org.apache.avro", module = "trevni-core")
    }

    api(libs.avro)
}

tasks.jar {
    enabled = true
}
