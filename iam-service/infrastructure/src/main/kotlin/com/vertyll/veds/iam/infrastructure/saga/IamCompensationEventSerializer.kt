package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.saga.SagaCompensationEvent
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventSerializer

/**
 * Type-safe Avro serializer for iam-service saga compensation events.
 *
 * Builds a generated [SagaCompensationEvent] SpecificRecord and delegates to
 * the Confluent Avro serializer (which registers the schema in the Schema Registry).
 */
class IamCompensationEventSerializer(
    private val avroPayloadSerializer: AvroPayloadSerializer,
    private val topic: String,
) : CompensationEventSerializer {
    override fun serializeCompensationEvent(
        sagaId: String,
        stepId: Long?,
        action: String,
        extraPayload: Map<String, Any?>,
    ): ByteArray {
        val record =
            SagaCompensationEvent
                .newBuilder()
                .setSagaId(sagaId)
                .setStepId(stepId)
                .setAction(action)
                .setExtraPayload(extraPayload.mapValues { it.value?.toString() ?: "" })
                .build()
        return avroPayloadSerializer.serialize(topic, record)
    }
}
