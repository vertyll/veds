package com.vertyll.projecta.sharedinfrastructure.saga.enums

enum class SagaStepStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATED,
    PARTIALLY_COMPLETED,
}
