package com.vertyll.projecta.mail.domain.service

import com.vertyll.projecta.mail.domain.model.entity.Saga
import com.vertyll.projecta.mail.domain.model.entity.SagaStep
import com.vertyll.projecta.mail.domain.model.enums.SagaCompensationActions
import com.vertyll.projecta.mail.domain.model.enums.SagaStatus
import com.vertyll.projecta.mail.domain.model.enums.SagaStepNames
import com.vertyll.projecta.mail.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.mail.domain.model.enums.SagaTypes
import com.vertyll.projecta.mail.domain.repository.SagaRepository
import com.vertyll.projecta.mail.domain.repository.SagaStepRepository
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaTopicNames
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * Manages the state of sagas and coordinates compensating transactions.
 */
@Service
class SagaManager(
    private val sagaRepository: SagaRepository,
    private val sagaStepRepository: SagaStepRepository,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val EMAIL_CANNOT_BE_UNSENT = "Email cannot be unsent, compensation logged for auditing purposes"
        private const val TEMPLATE_UPDATE_COMPENSATION_LOGGED = "Template update compensation logged"
    }

    private val sagaStepDefinitions =
        mapOf(
            SagaTypes.EMAIL_SENDING.value to
                listOf(
                    SagaStepNames.PROCESS_TEMPLATE.value,
                    SagaStepNames.SEND_EMAIL.value,
                ),
            SagaTypes.EMAIL_BATCH_PROCESSING.value to
                listOf(
                    SagaStepNames.PROCESS_TEMPLATE.value,
                    SagaStepNames.SEND_EMAIL.value,
                    SagaStepNames.RECORD_EMAIL_LOG.value,
                ),
            SagaTypes.TEMPLATE_MANAGEMENT.value to
                listOf(
                    SagaStepNames.TEMPLATE_UPDATE.value,
                ),
        )

    /**
     * Starts a new saga
     * @param sagaType The type of saga to start
     * @param payload Additional data related to the saga
     * @return The created saga instance
     */
    @Transactional
    fun startSaga(
        sagaType: SagaTypes,
        payload: Any,
    ): Saga {
        val payloadJson = payload as? String ?: objectMapper.writeValueAsString(payload)

        val saga =
            Saga(
                id = UUID.randomUUID().toString(),
                type = sagaType.value,
                status = SagaStatus.STARTED,
                payload = payloadJson,
                startedAt = Instant.now(),
            )

        return sagaRepository.save(saga)
    }

    /**
     * Records a step in a saga
     * @param sagaId The ID of the saga
     * @param stepName The name of the step
     * @param status The status of the step
     * @param payload Additional data related to the step
     * @return The created saga step
     */
    @Transactional
    fun recordSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Any? = null,
    ): SagaStep {
        val existingSteps = sagaStepRepository.findBySagaIdAndStepName(sagaId, stepName.value)
        if (existingSteps.isNotEmpty()) {
            val existingStep = existingSteps.first()
            logger.info("Saga step $stepName already exists for saga $sagaId, status: ${existingStep.status}")
            return existingStep
        }

        val payloadJson =
            payload?.let {
                it as? String ?: objectMapper.writeValueAsString(it)
            }

        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with ID $sagaId not found")
            }

        val step =
            SagaStep(
                sagaId = sagaId,
                stepName = stepName.value,
                status = status,
                payload = payloadJson,
                createdAt = Instant.now(),
            )

        val savedStep = sagaStepRepository.save(step)

        if (status == SagaStepStatus.COMPLETED) {
            savedStep.completedAt = Instant.now()
            sagaStepRepository.save(savedStep)
        }

        if (status == SagaStepStatus.FAILED) {
            saga.status = SagaStatus.COMPENSATING
            saga.lastError = "Step $stepName failed"
            saga.updatedAt = Instant.now()
            sagaRepository.save(saga)

            triggerCompensation(saga)
        } else if (status == SagaStepStatus.COMPLETED || status == SagaStepStatus.PARTIALLY_COMPLETED) {
            saga.updatedAt = Instant.now()

            if (status != SagaStepStatus.PARTIALLY_COMPLETED && areAllStepsCompleted(saga)) {
                saga.status = SagaStatus.COMPLETED
                saga.completedAt = Instant.now()
                logger.info("All steps completed for saga ${saga.id}, marking as COMPLETED")
            } else if (status == SagaStepStatus.PARTIALLY_COMPLETED) {
                saga.status = SagaStatus.PARTIALLY_COMPLETED
                saga.completedAt = Instant.now()
                logger.info("Saga ${saga.id} partially completed")
            }

            sagaRepository.save(saga)
        }

        return savedStep
    }

    /**
     * Checks if all expected steps for a saga have been completed
     * @param saga The saga to check
     * @return True if all expected steps are completed, false otherwise
     */
    private fun areAllStepsCompleted(saga: Saga): Boolean {
        val expectedSteps = sagaStepDefinitions[saga.type] ?: return false

        val completedSteps =
            sagaStepRepository.findBySagaIdAndStatus(
                saga.id,
                SagaStepStatus.COMPLETED,
            )

        val completedStepNames = completedSteps.map { it.stepName }

        return expectedSteps.all { expectedStep ->
            completedStepNames.contains(expectedStep)
        }
    }

    /**
     * Triggers compensation for a failed saga
     * @param saga The saga to compensate
     * @return Unit
     */
    private fun triggerCompensation(saga: Saga) {
        val completedSteps =
            sagaStepRepository
                .findBySagaIdAndStatus(
                    saga.id,
                    SagaStepStatus.COMPLETED,
                ).sortedByDescending { it.createdAt }

        logger.info("Triggering compensation for saga ${saga.id} with ${completedSteps.size} steps to compensate")

        completedSteps.forEach { step ->
            try {
                logger.info("Compensating step ${step.stepName} (ID: ${step.id}) for saga ${saga.id}")

                when (step.stepName) {
                    SagaStepNames.SEND_EMAIL.value -> compensateSendEmail(saga.id, step)
                    SagaStepNames.RECORD_EMAIL_LOG.value -> compensateRecordEmailLog(saga.id, step)
                    SagaStepNames.TEMPLATE_UPDATE.value -> compensateTemplateUpdate(saga.id, step)
                    else -> logger.warn("No compensation defined for step ${step.stepName}")
                }

                val compensationStep =
                    SagaStep(
                        sagaId = saga.id,
                        stepName = SagaStepNames.compensationNameFromString(step.stepName),
                        status = SagaStepStatus.STARTED,
                        createdAt = Instant.now(),
                    )
                val savedCompensationStep = sagaStepRepository.save(compensationStep)

                step.compensationStepId = savedCompensationStep.id
                sagaStepRepository.save(step)
            } catch (e: Exception) {
                logger.error("Failed to create compensation event for step ${step.stepName}: ${e.message}", e)
            }
        }

        saga.status = SagaStatus.COMPENSATING
        sagaRepository.save(saga)
    }

    /**
     * Compensate for sending an email (for auditing/logging only, can't "unsend" an email)
     * @param sagaId The ID of the saga
     * @param step The saga step that needs compensation
     * @return Unit
     */
    private fun compensateSendEmail(
        sagaId: String,
        step: SagaStep,
    ) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val to = payload["to"]?.toString()
            val emailId = payload["emailId"]?.toString()

            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload =
                    mapOf(
                        "sagaId" to sagaId,
                        "stepId" to step.id,
                        "action" to SagaCompensationActions.LOG_EMAIL_COMPENSATION.value,
                        "emailId" to emailId,
                        "to" to to,
                        "message" to EMAIL_CANNOT_BE_UNSENT,
                    ),
                sagaId = sagaId,
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for SendEmail: ${e.message}", e)
        }
    }

    /**
     * Compensate for recording an email log (delete it)
     * @param sagaId The ID of the saga
     * @param step The saga step that needs compensation
     * @return Unit
     */
    private fun compensateRecordEmailLog(
        sagaId: String,
        step: SagaStep,
    ) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val logId = payload["logId"]?.toString()

            if (logId != null) {
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.SAGA_COMPENSATION,
                    key = sagaId,
                    payload =
                        mapOf(
                            "sagaId" to sagaId,
                            "stepId" to step.id,
                            "action" to SagaCompensationActions.DELETE_EMAIL_LOG.value,
                            "logId" to logId,
                        ),
                    sagaId = sagaId,
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for RecordEmailLog: ${e.message}", e)
        }
    }

    /**
     * Compensate for updating a template (mostly for logging)
     * @param sagaId The ID of the saga
     * @param step The saga step that needs compensation
     * @return Unit
     */
    private fun compensateTemplateUpdate(
        sagaId: String,
        step: SagaStep,
    ) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val templateName = payload["templateName"]?.toString()

            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload =
                    mapOf(
                        "sagaId" to sagaId,
                        "stepId" to step.id,
                        "action" to SagaCompensationActions.LOG_TEMPLATE_COMPENSATION.value,
                        "templateName" to templateName,
                        "message" to TEMPLATE_UPDATE_COMPENSATION_LOGGED,
                    ),
                sagaId = sagaId,
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for TemplateUpdate: ${e.message}", e)
        }
    }
}
