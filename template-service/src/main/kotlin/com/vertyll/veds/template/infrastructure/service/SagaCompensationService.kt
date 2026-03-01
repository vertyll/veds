package com.vertyll.veds.template.infrastructure.service

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.template.domain.service.SagaManager
import com.vertyll.veds.sharedinfrastructure.saga.service.BaseSagaCompensationService
import com.vertyll.veds.template.domain.model.entity.SagaStep
import com.vertyll.veds.template.domain.model.enums.SagaCompensationActions
import com.vertyll.veds.template.domain.repository.SagaStepRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
class SagaCompensationService(
    sagaStepRepository: SagaStepRepository,
    objectMapper: ObjectMapper,
) : BaseSagaCompensationService<SagaStep>(sagaStepRepository, objectMapper) {
//    @KafkaListener(topics = [SagaManager.SAGA_COMPENSATION_TOPIC])
    override fun handleCompensationEvent(payload: String) = super.handleCompensationEvent(payload)

    override fun createCompensationStepEntity(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        createdAt: Instant,
        completedAt: Instant?,
        compensationStepId: Long?,
    ): SagaStep =
        SagaStep(
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
    ) {
        when (action) {
            SagaCompensationActions.EXAMPLE_COMPENSATION.value -> {
                logger.info("Example compensation for saga $sagaId")
            }
            else -> logger.warn("Unknown compensation action: $action")
        }
    }
}
