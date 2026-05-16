package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.contract.Saga
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStep

/**
 * Domain-specific compensation hook used by [SagaEngine].
 *
 * Implementations decide what to do for each step that needs to be undone.
 * Typically, compensation publishes a compensation event via
 * [SagaCompensationContext.publishCompensationEvent] which is then consumed by
 * a service-local Kafka listener.
 *
 * Works against the persistence-agnostic [Saga] / [SagaStep] contracts so it
 * is independent of the underlying storage technology.
 */
@Suppress("kotlin:S6517")
interface SagaCompensator<S : Saga<S>, T : SagaStep<T>> {
    fun compensateStep(
        saga: S,
        step: T,
        context: SagaCompensationContext,
    )
}
