package com.vertyll.veds.shared.saga

import java.time.Instant

/**
 * A read-only copy of a saga's state, handed to the application layer.
 *
 * The live aggregate is [Saga], implemented by whatever the storage adapter
 * persists. An application service is told what a saga *is* right now and never
 * transitions it directly — every transition goes through [SagaProcessPort], so
 * the engine stays the only writer and a caller cannot leave a saga in a state
 * the engine did not put it in.
 */
data class SagaSnapshot(
    /** The identifier of the saga. */
    val id: String,
    /** The kind of saga. */
    val type: String,
    /** Where the saga stands. */
    val status: SagaStatus,
    /** The data the saga carries, serialized. */
    val payload: String,
    /** The failure that stopped the saga, if one did. */
    val lastError: String? = null,
    /** When the saga started. */
    val startedAt: Instant = Instant.now(),
    /** When the saga finished, if it has. */
    val completedAt: Instant? = null,
    /** When the saga last changed. */
    val updatedAt: Instant = Instant.now(),
    /** The optimistic-locking version of the stored saga. */
    val version: Long? = null,
)
