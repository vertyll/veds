package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Service-provided serializer for compensation events that flow on each
 * microservice's internal `saga-compensation-<participant>` topic.
 *
 * The wire format (Avro / JSON / Protobuf) is an implementation detail of
 * the owning service; the saga engine consumes only the [ByteArray] result.
 * Paired with [CompensationEventDeserializer] on the consuming side.
 */
@Suppress("kotlin:S6517")
interface CompensationEventSerializer {
    /**
     * Serializes a compensation event envelope.
     *
     * @param sagaId originating saga's id (used for Saga Log Correlation on the consumer side).
     * @param stepId optional id of the step being compensated; `null` for saga-level actions.
     * @param action compensation action discriminator (e.g. `"DELETE_USER"`).
     * @param extraPayload free-form per-action payload merged into the envelope.
     */
    fun serializeCompensationEvent(
        sagaId: String,
        stepId: Long?,
        action: String,
        extraPayload: Map<String, Any?>,
    ): ByteArray
}
