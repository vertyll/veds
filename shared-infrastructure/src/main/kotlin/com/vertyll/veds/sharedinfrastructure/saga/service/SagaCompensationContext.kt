package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Callback surface that [SagaCompensator] uses to interact with the saga
 * engine while staying decoupled from it. Hides Kafka outbox and JSON
 * serialization details behind a small, intention-revealing API.
 */
interface SagaCompensationContext {
    /**
     * Publishes a compensation event to the service-local compensation topic.
     */
    fun publishCompensationEvent(
        sagaId: String,
        stepId: Long?,
        action: String,
        extraPayload: Map<String, Any?> = emptyMap(),
    )

    /**
     * Deserializes the step payload (JSON) into a [Map].
     * Returns an empty map when the payload is null/blank.
     */
    fun readStepPayload(payload: String?): Map<String, Any?>
}
