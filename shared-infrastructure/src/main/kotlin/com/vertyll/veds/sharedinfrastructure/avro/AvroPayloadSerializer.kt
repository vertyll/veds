package com.vertyll.veds.sharedinfrastructure.avro

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaInfraProperties
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.springframework.kafka.core.KafkaTemplate

/**
 * Thin Kotlin wrapper around Confluent's [KafkaAvroSerializer] configured
 * with the Schema Registry URL taken from [KafkaInfraProperties].
 *
 * Serializes any Avro `SpecificRecord` / `GenericRecord` value to the
 * Confluent wire format (`[magic byte][schema-id][avro payload]`) so that
 * the resulting [ByteArray] can be handed to the standard
 * [ByteArraySerializer]-based [KafkaTemplate] used by the outbox
 * processor. Paired with [AvroPayloadDeserializer] on the consumer side.
 */
class AvroPayloadSerializer(
    properties: KafkaInfraProperties,
) {
    private val serializer =
        KafkaAvroSerializer().apply {
            configure(
                mapOf(
                    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to properties.schemaRegistryUrl,
                ),
                false,
            )
        }

    /**
     * Serializes [value] for the given [topic] and returns the Confluent
     * Avro wire-format bytes (registers the schema in Schema Registry on
     * first use). [value] must be an Avro `SpecificRecord` or
     * `GenericRecord`.
     */
    fun serialize(
        topic: String,
        value: Any,
    ): ByteArray = serializer.serialize(topic, value)
}
