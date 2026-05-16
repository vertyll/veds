package com.vertyll.veds.sharedinfrastructure.saga.enums

/**
 * Lifecycle status of a saga aggregate (`Saga`).
 *
 * State machine (choreography flow):
 * ```
 *   STARTED ──► AWAITING_RESPONSE ──► COMPLETED                       (happy path)
 *      │                  │
 *      │                  └──► COMPENSATING ──► COMPENSATED           (rollback OK)
 *      │                                │
 *      │                                └──► COMPENSATION_FAILED      (manual intervention)
 *      │
 *      └──► FAILED                                                    (no compensation needed)
 * ```
 *
 * [isTerminal] returns `true` for states that the engine and watchdog must
 * never advance further on their own.
 */
enum class SagaStatus {
    /** Initial state. Local saga and its first step were just recorded. */
    STARTED,

    /**
     * Saga is parked waiting for an external feedback event correlated by
     * `sagaId` (Saga Log Correlation). Watched by `SagaWatchdog`, which
     * fails the saga if it stays here longer than `veds.saga.await-response-timeout`.
     */
    AWAITING_RESPONSE,

    /** Terminal — saga succeeded end-to-end. */
    COMPLETED,

    /** Terminal — saga failed and was *not* eligible for compensation. */
    FAILED,

    /**
     * Compensation in progress. `SagaCompensationRunner` is iterating
     * previously-completed steps in reverse order, undoing each one via
     * `SagaCompensator`.
     */
    COMPENSATING,

    /** Terminal — every step was successfully compensated. */
    COMPENSATED,

    /**
     * Terminal — at least one step refused to compensate. `SagaWatchdog`
     * will retry compensation after `veds.saga.compensation-retry-cooldown`.
     */
    COMPENSATION_FAILED,
    ;

    /** True when the engine must not perform any further automatic transitions on this saga. */
    fun isTerminal(): Boolean = this in setOf(COMPLETED, FAILED, COMPENSATED, COMPENSATION_FAILED)
}
