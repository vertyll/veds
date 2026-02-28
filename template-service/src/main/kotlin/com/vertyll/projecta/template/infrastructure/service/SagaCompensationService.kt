package com.vertyll.projecta.template.infrastructure.service

import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.projecta.template.domain.model.entity.SagaStep
import com.vertyll.projecta.template.domain.model.enums.SagaStepNames
import com.vertyll.projecta.template.domain.repository.SagaStepRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
class SagaCompensationService(
    private val sagaStepRepository: SagaStepRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Listens for compensation events and processes them
     */
    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getSagaCompensationTopic()}"])
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

            when (actionStr) { // NOSONAR
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
                            createdAt = Instant.now(),
                            completedAt = Instant.now(),
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
