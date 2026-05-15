package com.vertyll.veds.sharedinfrastructure.saga.contract

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import java.time.Instant

/**
 * Persistence-agnostic contract for a saga aggregate.
 *
 * Implementations can be backed by any storage technology (JPA, MongoDB,
 * DynamoDB, in-memory, …). The engine and adapters only depend on this
 * interface, never on a concrete entity class.
 *
 * All fields are exposed as read-only (`val`). State transitions are
 * performed exclusively through behavior methods that encapsulate the
 * aggregate's invariants (DDD: *rich aggregate methods*, Vernon, IDDD).
 *
 * Implementations decide internally how to realize the transition:
 *  - JPA-backed entities may mutate their `var` fields and return `this`
 *    (relying on Hibernate dirty-tracking).
 *  - Immutable documents (Mongo, in-memory) may return a fresh instance via
 *    `copy(...)`.
 * The engine never observes this difference.
 */
interface Saga {
    val id: String
    val type: String
    val status: SagaStatus
    val payload: String
    val lastError: String?
    val startedAt: Instant
    val completedAt: Instant?
    val updatedAt: Instant
    val version: Long?

    /** Transitions to [SagaStatus.COMPLETED] and stamps `completedAt`/`updatedAt`. */
    fun markCompleted(): Saga

    /** Transitions to [SagaStatus.AWAITING_RESPONSE] and stamps `updatedAt`. */
    fun markAwaitingResponse(): Saga

    /**
     * Transitions to [SagaStatus.FAILED], stores [error] as `lastError`, and
     * stamps `completedAt`/`updatedAt`.
     */
    fun markFailed(error: String): Saga

    /**
     * Transitions to [SagaStatus.COMPENSATING], stores [error] as `lastError`,
     * and stamps `updatedAt`. Used when a step failure triggers compensation
     * but the saga has not yet completed compensation.
     */
    fun startCompensating(error: String): Saga

    /** Transitions to [SagaStatus.COMPENSATED] and stamps `completedAt`/`updatedAt`. */
    fun markCompensated(): Saga

    /** Transitions to [SagaStatus.COMPENSATION_FAILED] and stamps `completedAt`/`updatedAt`. */
    fun markCompensationFailed(): Saga
}
