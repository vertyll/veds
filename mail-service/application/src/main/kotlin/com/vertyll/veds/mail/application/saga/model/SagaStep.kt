package com.vertyll.veds.mail.application.saga.model

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import java.time.Instant

data class SagaStep(
    val id: Long? = null,
    val sagaId: String,
    val stepName: String,
    val status: SagaStepStatus = SagaStepStatus.STARTED,
    val payload: String? = null,
    val errorMessage: String? = null,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val compensationStepId: Long? = null,
    val version: Long? = null,
) {
    fun markAs(
        newStatus: SagaStepStatus,
        error: String? = errorMessage,
    ): SagaStep =
        this.copy(
            status = newStatus,
            errorMessage = error,
            completedAt =
                if (newStatus in
                    listOf(SagaStepStatus.COMPLETED, SagaStepStatus.FAILED, SagaStepStatus.COMPENSATED)
                ) {
                    Instant.now()
                } else {
                    this.completedAt
                },
        )
}
