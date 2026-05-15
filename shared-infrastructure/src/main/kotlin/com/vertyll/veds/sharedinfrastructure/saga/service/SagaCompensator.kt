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
@Suppress("kotlin:S6517")
interface SagaCompensator<S : BaseSaga, T : BaseSagaStep> {
    fun compensateStep(
        saga: S,
        step: T,
        context: SagaCompensationContext,
    )
}
