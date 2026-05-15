package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSaga
import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep

/**
 * Domain-specific compensation hook used by [SagaEngine].
 *
 * Implementations decide what to do for each step that needs to be undone.
 * Typically, compensation publishes a compensation event via
 * [SagaCompensationContext.publishCompensationEvent] which is then consumed by
 * a service-local Kafka listener.
 *
 * Replaces inheritance (Template Method on `BaseSagaManager.compensateStep`)
 * with composition.
 */
interface SagaCompensator<S : BaseSaga, T : BaseSagaStep> {
    fun compensateStep(
        saga: S,
        step: T,
        context: SagaCompensationContext,
    )
}

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
