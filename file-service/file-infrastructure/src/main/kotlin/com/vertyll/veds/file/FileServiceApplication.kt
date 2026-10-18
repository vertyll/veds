package com.vertyll.veds.file

import com.vertyll.veds.file.infrastructure.storage.ObjectStorageProperties
import com.vertyll.veds.shared.translation.client.TranslationClientProperties
import com.vertyll.veds.shared.web.config.SharedConfigAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@Import(
    SharedConfigAutoConfiguration::class,
)
@ComponentScan(
    "com.vertyll.veds.file",
    "com.vertyll.veds.shared.messaging",
    "com.vertyll.veds.shared.translation.client",
)
@EnableJpaRepositories(
    "com.vertyll.veds.file.infrastructure.persistence.repository",
    "com.vertyll.veds.shared.messaging.kafka.persistence.outbox",
    "com.vertyll.veds.shared.messaging.kafka.persistence.inbox",
)
@EntityScan(
    "com.vertyll.veds.file.infrastructure.persistence.entity",
    "com.vertyll.veds.shared.messaging.kafka.persistence.outbox",
    "com.vertyll.veds.shared.messaging.kafka.persistence.inbox",
)
@EnableKafka
@EnableConfigurationProperties(
    ObjectStorageProperties::class,
    TranslationClientProperties::class,
)
class FileServiceApplication

fun main(args: Array<String>) {
    runApplication<FileServiceApplication>(*args)
}
