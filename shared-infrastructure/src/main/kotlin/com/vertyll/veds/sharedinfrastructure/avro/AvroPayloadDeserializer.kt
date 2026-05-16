package com.vertyll.veds.sharedinfrastructure.avro

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaInfraProperties
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer

/**
 * Thin Kotlin wrapper around Confluent's [KafkaAvroDeserializer] configured
 * with the Schema Registry URL taken from [KafkaInfraProperties] and
 * `specific.avro.reader=true` so the returned value is the generated
 * `SpecificRecord` subclass (and not a generic record).
 *
 * Paired with [AvroPayloadSerializer]; together they let any consumer of
 * the shared infrastructure work with raw `ByteArray` payloads on the wire
 * while still benefiting from Schema-Registry-backed Avro typing in
 * application code.
 */
class AvroPayloadDeserializer(
    properties: KafkaInfraProperties,
) {
    private val deserializer =
        KafkaAvroDeserializer().apply {
            configure(
                mapOf(
                    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to properties.schemaRegistryUrl,
                    "specific.avro.reader" to true,
                ),
                false,
            )
        }

    /**
     * Deserializes the Confluent Avro wire-format [payload] received from
     * [topic]. The returned value is a `SpecificRecord` subclass when one
     * is on the classpath, otherwise a `GenericRecord`.
     */
    fun deserialize(
        topic: String,
        payload: ByteArray,
    ): Any = deserializer.deserialize(topic, payload)
}
