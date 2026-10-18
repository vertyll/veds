package com.vertyll.veds.mail

import com.vertyll.veds.mail.infrastructure.config.MailProperties
import com.vertyll.veds.shared.authz.client.AuthzClientProperties
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
    "com.vertyll.veds.mail",
    "com.vertyll.veds.shared.messaging",
    "com.vertyll.veds.shared.translation.client",
    "com.vertyll.veds.shared.authz.client",
)
@EnableJpaRepositories(
    "com.vertyll.veds.mail.infrastructure.persistence.repository",
    "com.vertyll.veds.shared.messaging.kafka.persistence.outbox",
    "com.vertyll.veds.shared.messaging.kafka.persistence.inbox",
)
@EntityScan(
    "com.vertyll.veds.mail.infrastructure.persistence.entity",
    "com.vertyll.veds.shared.messaging.kafka.persistence.outbox",
    "com.vertyll.veds.shared.messaging.kafka.persistence.inbox",
)
@EnableKafka
@EnableConfigurationProperties(TranslationClientProperties::class, AuthzClientProperties::class, MailProperties::class)
class MailServiceApplication

fun main(args: Array<String>) {
    runApplication<MailServiceApplication>(*args)
}
