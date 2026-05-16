package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Service-provided serializer for compensation events that flow on each
 * microservice's internal `saga-compensation-<participant>` topic.
 *
 * The wire format (Avro / JSON / Protobuf) is an implementation detail of
 * the owning service; the saga engine consumes only the resulting
 * [ByteArray]. Paired with [CompensationCommandDeserializer] on the
 * consuming side.
 *
 * The type parameter [TCommand] is the **service-local, strongly-typed**
 * compensation command (typically a Kotlin `sealed interface` mirroring
 * the Avro tagged union). Keeping it generic means the shared engine
 * never speaks in `Map<String, Any?>` and there are no stringly-typed
 * action discriminators at the integration boundary.
 */
@Suppress("kotlin:S6517")
fun interface CompensationEventSerializer<TCommand : Any> {
    /**
     * Serializes a compensation event envelope.
     *
     * @param sagaId originating saga's id (used for Saga Log Correlation on the consumer side).
     * @param stepId optional id of the step being compensated; `null` for saga-level actions.
     * @param command typed compensation command to be encoded on the wire.
     */
    fun serialize(
        sagaId: String,
        stepId: Long?,
        command: TCommand,
    ): ByteArray
}
