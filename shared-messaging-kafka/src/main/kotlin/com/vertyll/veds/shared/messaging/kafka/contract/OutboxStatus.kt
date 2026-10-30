package com.vertyll.veds.shared.messaging.kafka.contract

/**
 * Persistence-agnostic status of an outbox message.
 *
 * Lifecycle:
 * ```
 *   PENDING ──► PROCESSING ──► COMPLETED                 (happy path)
 *                  │
 *                  └──► PENDING (retry, with retryCount++ and lastRetryAt)
 *                                    │
 *                                    └──► DEAD_LETTERED   (max retries exhausted)
 * ```
 */
enum class OutboxStatus {
    /** Waiting to be published, for the first time or again after a failure. */
    PENDING,

    /** Claimed by a dispatcher. */
    PROCESSING,

    /** Accepted by the broker. */
    COMPLETED,

    /** Terminal — max retries exhausted, requires manual intervention. */
    DEAD_LETTERED,
}
