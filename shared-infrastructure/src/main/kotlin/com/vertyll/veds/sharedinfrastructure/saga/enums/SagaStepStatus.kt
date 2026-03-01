package com.vertyll.veds.sharedinfrastructure.saga.enums

enum class SagaStepStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATED,
    COMPENSATION_FAILED,
    PARTIALLY_COMPLETED,
}
