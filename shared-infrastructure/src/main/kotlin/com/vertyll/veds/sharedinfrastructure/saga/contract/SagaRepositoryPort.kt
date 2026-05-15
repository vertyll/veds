package com.vertyll.veds.sharedinfrastructure.saga.contract

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import java.time.Instant

/**
 * Persistence-agnostic repository port for saga aggregates.
 *
 * Concrete adapters (JPA, MongoDB, …) implement this interface; the saga
 * engine has no awareness of the underlying storage technology.
 */
interface SagaRepositoryPort<S : Saga> {
    fun save(saga: S): S

    fun findOneById(id: String): S?

    fun findByType(type: String): List<S>

    fun findByStatus(status: SagaStatus): List<S>

    fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<S>

    fun findByStartedAtBefore(startedAt: Instant): List<S>

    fun findByStatusInAndStartedAtBefore(
        statuses: List<SagaStatus>,
        startedAt: Instant,
    ): List<S>
}
