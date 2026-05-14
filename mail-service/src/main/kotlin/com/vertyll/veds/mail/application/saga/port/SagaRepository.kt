package com.vertyll.veds.mail.application.saga.port

import com.vertyll.veds.mail.application.saga.model.Saga
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
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
