package com.vertyll.veds.sharedinfrastructure.saga.enums

enum class SagaStepStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATED,
    PARTIALLY_COMPLETED,
}
