package com.vertyll.veds.template

import com.vertyll.veds.shared.web.config.SharedConfigAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
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
    "com.vertyll.veds.template",
    "com.vertyll.veds.shared.messaging",
    "com.vertyll.veds.shared.translation.client",
)
@EnableJpaRepositories(
    "com.vertyll.veds.template.infrastructure.persistence.repository",
    "com.vertyll.veds.shared.messaging.kafka.persistence.outbox",
    "com.vertyll.veds.shared.messaging.kafka.persistence.inbox",
)
@EntityScan(
    "com.vertyll.veds.template.infrastructure.persistence.entity",
    "com.vertyll.veds.shared.messaging.kafka.persistence.outbox",
    "com.vertyll.veds.shared.messaging.kafka.persistence.inbox",
)
@EnableKafka
class TemplateServiceApplication

fun main(args: Array<String>) {
    runApplication<TemplateServiceApplication>(*args)
}
