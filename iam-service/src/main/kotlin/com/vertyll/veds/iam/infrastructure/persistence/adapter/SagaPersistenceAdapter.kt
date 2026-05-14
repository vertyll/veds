package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.application.saga.model.Saga
import com.vertyll.veds.iam.application.saga.port.SagaRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SagaPersistenceAdapter(
    private val repository: SagaJpaRepository,
) : SagaRepository {
    override fun save(saga: Saga): Saga = repository.save(saga.toJpaEntity()).toDomain()

    override fun findById(id: String): Saga? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<Saga> = repository.findByTypeAndStatus(type, status).map { it.toDomain() }

    override fun findByStatusAndUpdatedAtBefore(
        status: SagaStatus,
        updatedAt: Instant,
    ): List<Saga> = repository.findByStatusAndUpdatedAtBefore(status, updatedAt).map { it.toDomain() }
}

internal fun Saga.toJpaEntity() =
    SagaJpaEntity(
        id = this.id,
        type = this.type,
        status = this.status,
        payload = this.payload,
        lastError = this.lastError,
        startedAt = this.startedAt,
        completedAt = this.completedAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun SagaJpaEntity.toDomain() =
    Saga(
        id = this.id,
        type = this.type,
        status = this.status,
        payload = this.payload,
        lastError = this.lastError,
        startedAt = this.startedAt,
        completedAt = this.completedAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
