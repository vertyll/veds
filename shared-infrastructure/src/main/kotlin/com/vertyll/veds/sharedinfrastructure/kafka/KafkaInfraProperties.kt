package com.vertyll.veds.sharedinfrastructure.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Type-safe configuration for the shared Kafka infrastructure.
 *
 * Bound from `spring.kafka.*` in `application.yml`, mirroring the keys used by Spring Boot's
 * own Kafka configuration so existing `application-*.yml` files remain compatible.
 *
 * Replaces ad-hoc `@Value("${spring.kafka.bootstrap-servers:...}")` lookups and keeps the
 * style consistent with `MailProperties` (mail-service) and `SharedConfigProperties`.
 */
@ConfigurationProperties(prefix = "spring.kafka")
data class KafkaInfraProperties(
    /** Comma-separated list of Kafka broker addresses (host:port). */
    val bootstrapServers: String = "localhost:29092",
    val consumer: Consumer = Consumer(),
) {
    data class Consumer(
        /** Kafka consumer group id used by this service. */
        val groupId: String = "default-group",
        /** Where to start reading when no committed offset exists. */
        val autoOffsetReset: String = "earliest",
    )
}
