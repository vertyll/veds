package com.vertyll.veds.sharedinfrastructure.saga.enums

enum class SagaStatus {
    STARTED,
    AWAITING_RESPONSE,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED,
    ;

    fun isTerminal(): Boolean = this in setOf(COMPLETED, FAILED, COMPENSATED, COMPENSATION_FAILED)
}
