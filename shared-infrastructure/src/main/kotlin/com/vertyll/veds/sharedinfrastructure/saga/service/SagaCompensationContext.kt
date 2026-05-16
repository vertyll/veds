package com.vertyll.veds.sharedinfrastructure.saga.service

/**
 * Callback surface that [SagaCompensator] uses to interact with the saga
 * engine while staying decoupled from it. Hides Kafka outbox and JSON
 * serialization details behind a small, intention-revealing API.
 *
 * The type parameter [TCommand] is the service-local, strongly-typed
 * compensation command (typically a Kotlin `sealed interface` mirroring
 * the Avro tagged union) — keeping the engine free of `Map<String, Any?>`
 * envelopes and stringly-typed action discriminators.
 */
interface SagaCompensationContext<TCommand : Any> {
    /**
     * Publishes a typed compensation [command] to the service-local
     * compensation topic via the Transactional Outbox.
     */
    fun publishCompensationEvent(
        sagaId: String,
        stepId: Long?,
        command: TCommand,
    )

    /**
     * Deserializes the step payload (JSON, written by the saga engine on
     * step recording) into a [Map]. Returns an empty map when the
     * payload is null/blank. Use it inside [SagaCompensator] to read the
     * data captured at step-record time and assemble the typed [TCommand].
     */
    fun readStepPayload(payload: String?): Map<String, Any?>
}
