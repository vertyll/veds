package com.vertyll.projecta.mail.domain.service

import com.vertyll.projecta.mail.domain.model.entity.Saga
import com.vertyll.projecta.mail.domain.model.entity.SagaStep
import com.vertyll.projecta.mail.domain.model.enums.SagaCompensationActions
import com.vertyll.projecta.mail.domain.model.enums.SagaStepNames
import com.vertyll.projecta.mail.domain.model.enums.SagaTypes
import com.vertyll.projecta.mail.domain.repository.SagaRepository
import com.vertyll.projecta.mail.domain.repository.SagaStepRepository
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaTopicNames
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.projecta.sharedinfrastructure.saga.service.BaseSagaManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
class SagaManager(
    sagaRepository: SagaRepository,
    sagaStepRepository: SagaStepRepository,
    kafkaOutboxProcessor: KafkaOutboxProcessor,
    objectMapper: ObjectMapper,
) : BaseSagaManager<Saga, SagaStep>(
        sagaRepository,
        sagaStepRepository,
        kafkaOutboxProcessor,
        objectMapper,
    ) {
    private companion object {
        private const val EMAIL_CANNOT_BE_UNSENT = "Email cannot be unsent, compensation logged for auditing purposes"
        private const val TEMPLATE_UPDATE_COMPENSATION_LOGGED = "Template update compensation logged"
    }

    override fun getSagaStepDefinitions(): Map<String, List<String>> =
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

    override fun createSagaEntity(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ): Saga =
        Saga(
            id = id,
            type = type,
            status = status,
            payload = payload,
            startedAt = startedAt,
        )

    override fun createSagaStepEntity(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: String?,
        createdAt: Instant,
    ): SagaStep =
        SagaStep(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            payload = payload,
            createdAt = createdAt,
        )

    override fun compensateStep(
        saga: Saga,
        step: SagaStep,
    ) {
        when (step.stepName) {
            SagaStepNames.SEND_EMAIL.value -> compensateSendEmail(saga.id, step)
            SagaStepNames.RECORD_EMAIL_LOG.value -> compensateRecordEmailLog(saga.id, step)
            SagaStepNames.TEMPLATE_UPDATE.value -> compensateTemplateUpdate(saga.id, step)
            else -> logger.warn("No compensation defined for step ${step.stepName}")
        }
    }

    /**
     * Override to handle PARTIALLY_COMPLETED status
     */
    @Transactional
    override fun recordSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: Any?,
    ): SagaStep {
        val existingSteps = sagaStepRepository.findBySagaIdAndStepName(sagaId, stepName)
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
            createSagaStepEntity(
                sagaId = sagaId,
                stepName = stepName,
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

    // Convenience methods
    fun startSaga(
        sagaType: SagaTypes,
        payload: Any,
    ): Saga = startSaga(sagaType.value, payload)

    fun recordSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Any? = null,
    ): SagaStep = recordSagaStep(sagaId, stepName.value, status, payload)

    /**
     * Compensate for sending an email (for auditing/logging only, can't "unsend" an email)
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
