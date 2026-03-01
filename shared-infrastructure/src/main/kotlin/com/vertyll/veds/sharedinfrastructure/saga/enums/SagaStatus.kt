package com.vertyll.veds.sharedinfrastructure.saga.enums

enum class SagaStatus {
    STARTED,
    AWAITING_RESPONSE,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED,
    PARTIALLY_COMPLETED,
    ;

    fun isTerminal(): Boolean = this in setOf(COMPLETED, COMPENSATED, COMPENSATION_FAILED)
}
