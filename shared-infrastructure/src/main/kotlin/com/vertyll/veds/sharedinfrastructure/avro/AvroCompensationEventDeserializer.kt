package com.vertyll.veds.sharedinfrastructure.avro

import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventDeserializer
import org.apache.avro.generic.GenericRecord

/**
 * Avro-backed adapter for the persistence-agnostic
 * [CompensationEventDeserializer] port.
 *
 * Reads compensation events from a fixed [topic] using
 * [AvroPayloadDeserializer] (Confluent Schema Registry) and flattens the
 * resulting [GenericRecord] into the loose `Map<String, Any?>` envelope
 * that `SagaCompensationEngine` expects, with keys `sagaId`, `stepId`,
 * `action`, `extraPayload`.
 *
 * Used by per-service compensation listeners whose compensation contract
 * (see `contracts/<service>/saga-compensation/`) is encoded in Avro.
 */
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
