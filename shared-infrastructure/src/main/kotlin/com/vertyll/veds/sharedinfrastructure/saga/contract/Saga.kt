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
d *
 * The interface is **F-bounded** (`Saga<S : Saga<S>>`) so behavior methods
 * return the concrete adapter type [S]. This eliminates unchecked casts in
 * the engine while preserving the rich-aggregate contract.
 */
interface Saga<S : Saga<S>> {
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
    fun markCompleted(): S

    /** Transitions to [SagaStatus.AWAITING_RESPONSE] and stamps `updatedAt`. */
    fun markAwaitingResponse(): S

    /**
     * Transitions to [SagaStatus.FAILED], stores [error] as `lastError`, and
     * stamps `completedAt`/`updatedAt`.
     */
    fun markFailed(error: String): S

    /**
     * Transitions to [SagaStatus.COMPENSATING], stores [error] as `lastError`,
     * and stamps `updatedAt`. Used when a step failure triggers compensation
     * but the saga has not yet completed compensation.
     */
    fun startCompensating(error: String): S

    /** Transitions to [SagaStatus.COMPENSATED] and stamps `completedAt`/`updatedAt`. */
    fun markCompensated(): S

    /** Transitions to [SagaStatus.COMPENSATION_FAILED] and stamps `completedAt`/`updatedAt`. */
    fun markCompensationFailed(): S
}
