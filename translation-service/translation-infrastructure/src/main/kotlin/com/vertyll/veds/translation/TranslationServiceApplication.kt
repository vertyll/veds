package com.vertyll.veds.translation

import com.vertyll.veds.shared.authz.client.AuthzClientProperties
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import com.vertyll.veds.shared.messaging.kafka.OutboxDispatchTx
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxJpaAdapter
import com.vertyll.veds.shared.web.config.SharedConfigAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@Import(
    SharedConfigAutoConfiguration::class,
)
@ComponentScan(
    basePackages = [
        "com.vertyll.veds.translation",
        "com.vertyll.veds.shared.messaging",
        "com.vertyll.veds.shared.authz.client",
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [KafkaOutboxProcessor::class, OutboxDispatchTx::class, OutboxJpaAdapter::class],
        ),
    ],
)
@EnableJpaRepositories(
    "com.vertyll.veds.translation.infrastructure.persistence.repository",
    "com.vertyll.veds.shared.messaging.kafka.persistence.inbox",
)
@EntityScan(
    "com.vertyll.veds.translation.infrastructure.persistence.entity",
    "com.vertyll.veds.shared.messaging.kafka.persistence.inbox",
)
@EnableKafka
@EnableConfigurationProperties(AuthzClientProperties::class)
class TranslationServiceApplication

fun main(args: Array<String>) {
    runApplication<TranslationServiceApplication>(*args)
}
