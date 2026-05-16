package com.vertyll.veds.sharedinfrastructure.avro

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaInfraProperties
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer

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

    fun serialize(
        topic: String,
        value: Any,
    ): ByteArray = serializer.serialize(topic, value)
}
