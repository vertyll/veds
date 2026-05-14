package com.vertyll.veds.mail.application.saga.model

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import java.time.Instant

data class Saga(
    val id: String,
    val type: String,
    val status: SagaStatus = SagaStatus.STARTED,
    val payload: String,
    val lastError: String? = null,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    fun copyWithStatus(
        newStatus: SagaStatus,
        error: String? = lastError,
    ): Saga =
        this.copy(
            status = newStatus,
            lastError = error,
            updatedAt = Instant.now(),
            completedAt = if (newStatus == SagaStatus.COMPLETED || newStatus == SagaStatus.FAILED) Instant.now() else this.completedAt,
        )
}
