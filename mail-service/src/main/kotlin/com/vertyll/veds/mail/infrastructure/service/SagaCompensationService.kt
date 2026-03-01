package com.vertyll.veds.mail.infrastructure.service

import com.vertyll.veds.mail.domain.model.entity.SagaStep
import com.vertyll.veds.mail.domain.model.enums.SagaCompensationActions
import com.vertyll.veds.mail.domain.repository.SagaStepRepository
import com.vertyll.veds.mail.domain.service.SagaManager
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.BaseSagaCompensationService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
class SagaCompensationService(
    sagaStepRepository: SagaStepRepository,
    objectMapper: ObjectMapper,
) : BaseSagaCompensationService<SagaStep>(sagaStepRepository, objectMapper) {
    @KafkaListener(topics = [SagaManager.SAGA_COMPENSATION_TOPIC])
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
            SagaCompensationActions.LOG_EMAIL_COMPENSATION.value -> {
                val emailId = event["emailId"]?.toString()
                val to = event["to"]?.toString()
                val message = event["message"]?.toString() ?: "Email compensation recorded"

                logger.info("Email compensation logged for email to $to (ID: $emailId): $message")
            }
            SagaCompensationActions.LOG_TEMPLATE_COMPENSATION.value -> {
                val templateName = event["templateName"]?.toString()
                val message = event["message"]?.toString() ?: "Template compensation recorded"

                logger.info("Template compensation logged for template $templateName: $message")
            }
            SagaCompensationActions.DELETE_EMAIL_LOG.value -> {
                val logId = event["logId"]?.toString()
                logger.info("Email log deletion compensation for log ID: $logId")
            }
            else -> logger.warn("Unknown compensation action: $action")
        }
    }
}
