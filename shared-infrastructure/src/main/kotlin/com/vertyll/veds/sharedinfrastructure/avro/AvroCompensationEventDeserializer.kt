package com.vertyll.veds.sharedinfrastructure.avro

import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventDeserializer
import org.apache.avro.generic.GenericRecord

class AvroCompensationEventDeserializer(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val topic: String,
) : CompensationEventDeserializer {
    override fun deserializeCompensationEvent(payload: ByteArray): Map<String, Any?> {
        val record = avroPayloadDeserializer.deserialize(topic, payload) as GenericRecord
        val extraPayload =
            (record["extraPayload"] as? Map<*, *>)?.entries?.associate { (k, v) -> k.toString() to v?.toString() } ?: emptyMap()

        return mapOf(
            "sagaId" to record["sagaId"].toString(),
            "stepId" to (record["stepId"] as? Number)?.toLong(),
            "action" to record["action"].toString(),
            "extraPayload" to extraPayload,
        )
    }
}
