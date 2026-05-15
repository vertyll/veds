package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.contract.Saga
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import java.time.Instant

/**
 * Hook used by [SagaEngine] to instantiate concrete saga/saga-step instances
 * for the owning microservice.
 *
 * Each service provides its own implementation against the persistence-agnostic
 * [Saga] / [SagaStep] contracts. The factory is the only place that knows the
 * concrete persistence type (JPA entity, Mongo document, …); the engine itself
 * stays storage-agnostic.
 */
interface SagaEntityFactory<S : Saga, T : SagaStep> {
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
