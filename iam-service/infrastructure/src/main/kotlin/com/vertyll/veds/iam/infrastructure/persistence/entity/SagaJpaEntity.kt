package com.vertyll.veds.iam.infrastructure.persistence.entity

import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSaga
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "saga")
class SagaJpaEntity(
    id: String,
    type: String,
    status: SagaStatus = SagaStatus.STARTED,
    payload: String,
    lastError: String? = null,
    startedAt: Instant = Instant.now(),
    completedAt: Instant? = null,
    updatedAt: Instant = Instant.now(),
    version: Long? = null,
) : BaseSaga<SagaJpaEntity>(
        id = id,
        type = type,
        status = status,
        payload = payload,
        lastError = lastError,
        startedAt = startedAt,
        completedAt = completedAt,
        updatedAt = updatedAt,
        version = version,
    ) {
    override fun self(): SagaJpaEntity = this
}
