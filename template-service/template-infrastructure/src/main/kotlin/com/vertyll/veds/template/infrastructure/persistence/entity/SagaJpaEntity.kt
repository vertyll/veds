package com.vertyll.veds.template.infrastructure.persistence.entity

import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.engine.persistence.BaseSaga
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "saga")
internal class SagaJpaEntity(
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
