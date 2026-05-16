package com.vertyll.veds.sharedinfrastructure.kafka.contract

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
    PENDING,
    PROCESSING,
    COMPLETED,

    /** Terminal — max retries exhausted, requires manual intervention. */
    DEAD_LETTERED,
}
