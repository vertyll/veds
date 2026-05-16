package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.contract.Saga
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStep

/**
 * Domain-specific compensation hook used by [SagaEngine].
 *
 * Implementations decide what to do for each step that needs to be undone.
 * Typically, compensation publishes a typed compensation event via
 * [SagaCompensationContext.publishCompensationEvent] which is then consumed
 * by a service-local Kafka listener.
 *
 * Works against the persistence-agnostic [Saga] / [SagaStep] contracts so
 * it is independent of the underlying storage technology. The type
 * parameter [TCommand] is the service-local, strongly-typed compensation
 * command — keeping the engine free of stringly-typed dispatch.
 */
@Suppress("kotlin:S6517")
interface SagaCompensator<S : Saga<S>, T : SagaStep<T>, TCommand : Any> {
    /**
     * Compensates a single completed [step] of [saga]. Implementations
     * typically publish a typed compensation [TCommand] via
     * [SagaCompensationContext.publishCompensationEvent] so the local
     * Kafka listener performs the actual undo asynchronously.
     *
     * Should be idempotent — `SagaCompensationRunner` may invoke it again
     * after a partial failure.
     */
    fun compensateStep(
        saga: S,
        step: T,
        context: SagaCompensationContext<TCommand>,
    )
}
