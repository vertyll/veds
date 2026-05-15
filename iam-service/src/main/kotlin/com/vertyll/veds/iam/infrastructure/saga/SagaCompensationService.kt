package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.service.AuthCompensationService
import com.vertyll.veds.iam.infrastructure.config.SagaConfig
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.BaseSagaCompensationService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
internal class SagaCompensationService(
    private val authCompensationService: AuthCompensationService,
    sagaStepRepository: SagaStepJpaRepository,
    objectMapper: ObjectMapper,
) : BaseSagaCompensationService<SagaStepJpaEntity>(sagaStepRepository, objectMapper) {
    @KafkaListener(topics = [SagaConfig.SAGA_COMPENSATION_TOPIC])
    override fun handleCompensationEvent(payload: String) = super.handleCompensationEvent(payload)

    override fun createCompensationStepEntity(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        createdAt: Instant,
        completedAt: Instant?,
        compensationStepId: Long?,
    ): SagaStepJpaEntity =
        SagaStepJpaEntity(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            createdAt = createdAt,
            completedAt = completedAt,
            compensationStepId = compensationStepId,
        )

    override fun processCompensation(
        sagaId: String,
        action: String,
        event: Map<String, Any?>,
    ) = authCompensationService.compensate(action, event)
}
