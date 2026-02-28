package com.vertyll.veds.sharedinfrastructure.saga.enums

enum class SagaStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    PARTIALLY_COMPLETED,
}
