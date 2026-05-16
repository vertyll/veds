package com.vertyll.veds.sharedinfrastructure.saga.enums

/**
 * Lifecycle status of an individual saga step (`SagaStep`).
 *
 * Steps progress independently of the owning `Saga` status; the saga engine
 * uses step statuses to decide whether the saga as a whole can be marked
 * `COMPLETED` (all steps `COMPLETED`) or must be compensated (any
 * `FAILED`).
 *
 * Terminal statuses (`COMPLETED`, `FAILED`, `COMPENSATED`,
 * `COMPENSATION_FAILED`) prevent the engine from re-applying a different
 * status to the same step (idempotency guard inside `recordSagaStep`).
 */
enum class SagaStepStatus {
    /** Initial state, written by `SagaEngine.recordSagaStep` before applying the requested status. */
    STARTED,

    /** Terminal — step finished successfully. */
    COMPLETED,

    /** Terminal — step failed; triggers saga-level compensation. */
    FAILED,

    /** Terminal — step was successfully rolled back by `SagaCompensator`. */
    COMPENSATED,

    /** Terminal — compensator failed to roll back the step; eligible for watchdog retry. */
    COMPENSATION_FAILED,

    /**
     * Step succeeded only in part (rare; reserved for steps that publish to
     * multiple downstream effects and want to record partial success without
     * triggering full compensation).
     */
    PARTIALLY_COMPLETED,
}
