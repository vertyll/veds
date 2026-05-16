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
    /** Persists [saga] (insert or update) and returns the stored instance. */
    fun save(saga: S): S

    /** Looks up a saga by its [Saga.id]; `null` if absent. */
    fun findOneById(id: String): S?

    /** Returns all sagas of the given [Saga.type]. */
    fun findByType(type: String): List<S>

    /** Returns all sagas currently in the given [SagaStatus]. */
    fun findByStatus(status: SagaStatus): List<S>

    /** Returns sagas matching both [type] and [status]. */
    fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<S>

    /** Returns sagas whose [Saga.startedAt] is strictly before [startedAt]. */
    fun findByStartedAtBefore(startedAt: Instant): List<S>

    /** Returns sagas in any of [statuses] whose [Saga.startedAt] is strictly before [startedAt]. */
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
