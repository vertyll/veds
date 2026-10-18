package com.vertyll.veds.mail.infrastructure.persistence.entity

import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.engine.persistence.BaseSagaStep
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "saga_step",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["sagaId", "stepName"]),
    ],
)
internal class SagaStepJpaEntity(
    id: Long? = null,
    sagaId: String,
    stepName: String,
    status: SagaStepStatus,
    payload: String? = null,
    errorMessage: String? = null,
    createdAt: Instant,
    completedAt: Instant? = null,
    compensationStepId: Long? = null,
    version: Long? = null,
) : BaseSagaStep<SagaStepJpaEntity>(
        id = id,
        sagaId = sagaId,
        stepName = stepName,
        status = status,
        payload = payload,
        errorMessage = errorMessage,
        createdAt = createdAt,
        completedAt = completedAt,
        compensationStepId = compensationStepId,
        version = version,
    ) {
    override fun self(): SagaStepJpaEntity = this
}
