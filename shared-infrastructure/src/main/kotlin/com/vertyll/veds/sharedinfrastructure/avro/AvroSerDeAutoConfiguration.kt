package com.vertyll.veds.sharedinfrastructure.avro

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaInfraProperties
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Autoconfiguration registering the shared Avro serializer/deserializer
 * beans, conditional on `spring.kafka.schema-registry-url` being set so
 * services that do not use Avro on the wire incur no cost.
 *
 * The beans are infrastructure-only — Confluent's [KafkaAvroSerializer] /
 * Deserializer wrapped by [AvroPayloadSerializer] / [AvroPayloadDeserializer].
 * They are NOT registered as the `KafkaTemplate`'s value serializer (the
 * template stays `ByteArray`-based to fit the outbox pattern); callers
 * invoke them explicitly when assembling outbox payloads or decoding
 * inbound messages.
 */
@Configuration
@ConditionalOnProperty(
    name = ["spring.kafka.schema-registry-url"],
)
internal class AvroSerDeAutoConfiguration {
    /** Confluent Avro serializer wired to the configured Schema Registry. */
    @Bean
    fun avroPayloadSerializer(properties: KafkaInfraProperties) = AvroPayloadSerializer(properties)

    /** Confluent Avro deserializer wired to the configured Schema Registry. */
    @Bean
    fun avroPayloadDeserializer(properties: KafkaInfraProperties) = AvroPayloadDeserializer(properties)
}
