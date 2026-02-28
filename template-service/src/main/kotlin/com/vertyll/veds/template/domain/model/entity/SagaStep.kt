package com.vertyll.veds.template.domain.model.entity

import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
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
class SagaStep(
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
) : BaseSagaStep(
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
    constructor() : this(
        id = null,
        sagaId = "",
        stepName = "",
        status = SagaStepStatus.STARTED,
        payload = null,
        errorMessage = null,
        createdAt = Instant.now(),
        completedAt = null,
        compensationStepId = null,
        version = null,
    )
}
