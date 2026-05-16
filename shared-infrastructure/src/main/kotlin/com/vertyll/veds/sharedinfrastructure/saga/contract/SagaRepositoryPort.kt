package com.vertyll.veds.sharedinfrastructure.saga.contract

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import java.time.Instant

/**
 * Persistence-agnostic repository port for saga aggregates.
 *
 * Concrete adapters (JPA, MongoDB, …) implement this interface; the saga
 * engine has no awareness of the underlying storage technology.
 */
interface SagaRepositoryPort<S : Saga<S>> {
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

    /**
     * Finds sagas whose [Saga.status] is in [statuses] and whose
     * [Saga.updatedAt] is strictly before [updatedAt]. Used by the saga
     * watchdog to detect AWAITING_RESPONSE timeouts and to schedule
     * compensation retries with a cooldown.
     */
    fun findByStatusInAndUpdatedAtBefore(
        statuses: List<SagaStatus>,
        updatedAt: Instant,
    ): List<S>
}
