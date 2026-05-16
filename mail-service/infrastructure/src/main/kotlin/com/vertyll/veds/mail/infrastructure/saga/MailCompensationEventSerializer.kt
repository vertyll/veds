package com.vertyll.veds.mail.infrastructure.saga

import com.vertyll.veds.mail.saga.SagaCompensationEvent
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventSerializer

internal class MailCompensationEventSerializer(
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
