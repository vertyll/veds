package com.vertyll.veds.mail.infrastructure.service

import com.vertyll.veds.mail.domain.model.entity.SagaStep
import com.vertyll.veds.mail.domain.model.enums.SagaCompensationActions
import com.vertyll.veds.mail.domain.model.enums.SagaStepNames
import com.vertyll.veds.mail.domain.repository.SagaStepRepository
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicNames
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class SagaCompensationService(
    private val sagaStepRepository: SagaStepRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Listens for compensation events and processes them
     */
    @KafkaListener(topics = [KafkaTopicNames.Topics.SAGA_COMPENSATION])
    @Transactional
    fun handleCompensationEvent(payload: String) {
        try {
            val event =
                objectMapper.readValue(
                    payload,
                    Map::class.java,
                )
            val sagaId = event["sagaId"] as String
            val actionStr = event["action"] as String

            logger.info("Processing compensation action: $actionStr for saga $sagaId")

            when (actionStr) {
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
                else -> {
                    logger.warn("Unknown compensation action: $actionStr")
                }
            }

            val stepId = event["stepId"] as? Number
            if (stepId != null) {
                val step = sagaStepRepository.findById(stepId.toLong()).orElse(null)
                if (step != null) {
                    val compensationStep =
                        SagaStep(
                            sagaId = sagaId,
                            stepName = SagaStepNames.compensationNameFromString(step.stepName),
                            status = SagaStepStatus.COMPENSATED,
                            createdAt = java.time.Instant.now(),
                            completedAt = java.time.Instant.now(),
                            compensationStepId = step.id,
                        )
                    sagaStepRepository.save(compensationStep)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process compensation event: ${e.message}", e)
        }
    }
}
