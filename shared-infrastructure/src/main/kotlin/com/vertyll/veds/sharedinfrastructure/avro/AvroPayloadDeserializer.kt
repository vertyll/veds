package com.vertyll.veds.sharedinfrastructure.avro

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaInfraProperties
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer

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

    fun deserialize(
        topic: String,
        payload: ByteArray,
    ): Any = deserializer.deserialize(topic, payload)
}
