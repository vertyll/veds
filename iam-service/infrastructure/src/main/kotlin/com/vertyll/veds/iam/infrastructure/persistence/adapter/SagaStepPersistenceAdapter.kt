package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.application.port.out.SagaStepRepository
import com.vertyll.veds.iam.application.saga.model.SagaStep
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class SagaStepPersistenceAdapter(
    private val repository: SagaStepJpaRepository,
) : SagaStepRepository {
    override fun save(sagaStep: SagaStep): SagaStep = repository.save(sagaStep.toJpaEntity()).toDomain()

    override fun findById(id: Long): SagaStep? = repository.findByIdOrNull(id)?.toDomain()

    override fun findBySagaId(sagaId: String): List<SagaStep> = repository.findBySagaId(sagaId).map { it.toDomain() }

    override fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ): SagaStep? = repository.findBySagaIdAndStepName(sagaId, stepName).firstOrNull()?.toDomain()

    override fun findBySagaIdAndStepNameAndStatus(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
    ): List<SagaStep> = repository.findBySagaIdAndStepNameAndStatus(sagaId, stepName, status).map { it.toDomain() }

    override fun findBySagaIdOrderByCreatedAtDesc(sagaId: String): List<SagaStep> =
        repository.findBySagaIdOrderByCreatedAtDesc(sagaId).map { it.toDomain() }
}

private fun SagaStep.toJpaEntity() =
    SagaStepJpaEntity(
        id = this.id,
        sagaId = this.sagaId,
        stepName = this.stepName,
        status = this.status,
        payload = this.payload,
        errorMessage = this.errorMessage,
        createdAt = this.createdAt,
        completedAt = this.completedAt,
        compensationStepId = this.compensationStepId,
        version = this.version,
    )

private fun SagaStepJpaEntity.toDomain() =
    SagaStep(
        id = this.id,
        sagaId = this.sagaId,
        stepName = this.stepName,
        status = this.status,
        payload = this.payload,
        errorMessage = this.errorMessage,
        createdAt = this.createdAt,
        completedAt = this.completedAt,
        compensationStepId = this.compensationStepId,
        version = this.version,
    )
