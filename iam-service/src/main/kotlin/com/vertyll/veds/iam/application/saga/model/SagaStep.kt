package com.vertyll.veds.iam.application.saga.model

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import java.time.Instant

data class SagaStep(
    val id: Long? = null,
    val sagaId: String,
    val stepName: String,
    val status: SagaStepStatus,
    val payload: String? = null,
    val errorMessage: String? = null,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val compensationStepId: Long? = null,
    val version: Long? = null,
)
