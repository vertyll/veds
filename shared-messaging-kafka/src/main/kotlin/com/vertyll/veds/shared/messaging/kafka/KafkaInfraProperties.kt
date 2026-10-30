package com.vertyll.veds.shared.messaging.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Type-safe configuration for the shared Kafka infrastructure.
 *
 * Bound from `spring.kafka.*` in `application.yml`, mirroring the keys used by Spring Boot's
 * own Kafka configuration so existing `application-*.yml` files remain compatible.
 */
@ConfigurationProperties(prefix = "spring.kafka")
data class KafkaInfraProperties(
    /** Comma-separated list of Kafka broker addresses (host:port). */
    val bootstrapServers: String,
    /** Client properties passed through to Kafka, including `schema.registry.url` for Avro. */
    val properties: Map<String, String>,
    /** How clients authenticate to the brokers. */
    val security: Security,
    /** The trust settings of the TLS listener. */
    val ssl: Ssl,
    /** The settings of this service as a consumer. */
    val consumer: Consumer,
) {
    /** Schema Registry endpoint used by the Avro serializer and deserializer. */
    val schemaRegistryUrl: String
        get() = properties.getValue(SCHEMA_REGISTRY_URL)

    /** The settings of a service as a Kafka consumer. */
    data class Consumer(
        /** Kafka consumer group id used by this service. */
        val groupId: String,
        /** Where to start reading when no committed offset exists. */
        val autoOffsetReset: String,
    )

    /**
     * Broker connection security. Key names mirror Spring Boot's own
     * `spring.kafka.security.*` so env overrides bind identically
     * (SPRING_KAFKA_SECURITY_PROTOCOL / KAFKA_SECURITY_PROTOCOL via yml).
     */
    data class Security(
        /** PLAINTEXT (local dev) or SSL (cluster listener :9094). */
        val protocol: String,
    )

    /**
     * TLS trust for the SSL listener. Scoped to the Kafka client on purpose:
     * a global JVM truststore would break the services' public-TLS calls
     * (Keycloak, Resend). Mirrors `spring.kafka.ssl.*` key names.
     */
    data class Ssl(
        /** Plain filesystem path to the truststore, e.g. /tls-kafka/truststore.p12. */
        val trustStoreLocation: String,
        /** The format of the truststore, such as `PKCS12`. */
        val trustStoreType: String,
        /** The password of the truststore. */
        val trustStorePassword: String,
    )

    private companion object {
        const val SCHEMA_REGISTRY_URL = "schema.registry.url"
    }
}
