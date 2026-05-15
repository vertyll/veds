package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSaga
import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import java.time.Instant

/**
 * Hook used by [SagaEngine] to instantiate concrete saga/saga-step JPA entities
 * for the owning microservice.
 *
 * Each service provides its own implementation, because each service owns its
 * own `*JpaEntity` subclasses of [BaseSaga] / [BaseSagaStep] (so that JPA can
 * map them to per-service tables / migrations).
 *
 * Replaces inheritance (Template Method on `BaseSagaManager`) with composition.
 */
interface SagaEntityFactory<S : BaseSaga, T : BaseSagaStep> {
    fun createSaga(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ): S

    fun createSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: String?,
        createdAt: Instant,
    ): T
}
