package com.vertyll.veds.template.application.saga.port

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.template.application.saga.model.Saga
import java.time.Instant

interface SagaRepository {
    fun save(saga: Saga): Saga

    fun findById(id: String): Saga?

    fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<Saga>

    fun findByStatusAndUpdatedAtBefore(
        status: SagaStatus,
        updatedAt: Instant,
    ): List<Saga>
}
