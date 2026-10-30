package com.vertyll.veds.shared.saga

/**
 * Lifecycle of a saga instance.
 *
 * A saga either completes or is compensated; [COMPENSATION_FAILED] is the state
 * that needs a human, because the system could neither finish the work nor undo
 * it.
 *
 * [AWAITING_RESPONSE] is what makes the watchdog possible: a saga parked here
 * past its timeout is stuck, and nothing else distinguishes "waiting" from
 * "abandoned".
 */
enum class SagaStatus {
    /** The saga is running its steps. */
    STARTED,

    /** The saga waits for another service to answer. */
    AWAITING_RESPONSE,

    /** Every step succeeded. */
    COMPLETED,

    /** A step failed, and nothing is left to undo. */
    FAILED,

    /** The saga is undoing the steps already done. */
    COMPENSATING,

    /** Every completed step has been undone. */
    COMPENSATED,

    /** A compensation failed, so an operator has to step in. */
    COMPENSATION_FAILED,
    ;

    /** No further transition is possible, so the watchdog may stop looking at it. */
    fun isTerminal(): Boolean = this in setOf(COMPLETED, FAILED, COMPENSATED, COMPENSATION_FAILED)
}
