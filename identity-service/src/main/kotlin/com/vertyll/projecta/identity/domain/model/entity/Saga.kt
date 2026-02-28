package com.vertyll.projecta.identity.domain.model.entity

import com.vertyll.projecta.sharedinfrastructure.saga.entity.BaseSaga
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStatus
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "saga")
class Saga(
    id: String,
    type: String,
    status: SagaStatus,
    payload: String,
    lastError: String? = null,
    startedAt: Instant,
    completedAt: Instant? = null,
    updatedAt: Instant = Instant.now(),
    version: Long? = null,
) : BaseSaga(
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
    constructor() : this(
        id = "",
        type = "",
        status = SagaStatus.STARTED,
        payload = "",
        lastError = null,
        startedAt = Instant.now(),
        completedAt = null,
        updatedAt = Instant.now(),
        version = null,
    )
}
