package com.vertyll.veds.mail.domain.service

import com.vertyll.veds.mail.domain.model.entity.Saga
import com.vertyll.veds.mail.domain.model.entity.SagaStep
import com.vertyll.veds.mail.domain.model.enums.SagaCompensationActions
import com.vertyll.veds.mail.domain.model.enums.SagaStepNames
import com.vertyll.veds.mail.domain.repository.SagaRepository
import com.vertyll.veds.mail.domain.repository.SagaStepRepository
import com.vertyll.veds.sharedinfrastructure.event.EventSource
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.BaseSagaManager
import org.springframework.stereotype.Service
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
    override val serviceSource = EventSource.MAIL_SERVICE

    private companion object {
        const val EMAIL_CANNOT_BE_UNSENT = "Email cannot be unsent, compensation logged for auditing purposes"
        const val TEMPLATE_UPDATE_COMPENSATION_LOGGED = "Template update compensation logged"
    }

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
            else -> logger.warn("No compensation defined for step '${step.stepName}'")
        }
    }

    private fun compensateSendEmail(
        sagaId: String,
        step: SagaStep,
    ) {
        runCatching {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val to = payload["to"]?.toString()
            val emailId = payload["emailId"]?.toString()

            publishCompensationEvent(
                sagaId = sagaId,
                stepId = step.id,
                action = SagaCompensationActions.LOG_EMAIL_COMPENSATION.value,
                extraPayload =
                    mapOf(
                        "emailId" to emailId,
                        "to" to to,
                        "message" to EMAIL_CANNOT_BE_UNSENT,
                    ),
            )
        }.onFailure { e ->
            logger.error("Failed to publish compensation event for step '${SagaStepNames.SEND_EMAIL.value}': ${e.message}", e)
        }
    }

    private fun compensateRecordEmailLog(
        sagaId: String,
        step: SagaStep,
    ) {
        runCatching {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val logId = payload["logId"]?.toString() ?: return@runCatching

            publishCompensationEvent(
                sagaId = sagaId,
                stepId = step.id,
                action = SagaCompensationActions.DELETE_EMAIL_LOG.value,
                extraPayload = mapOf("logId" to logId),
            )
        }.onFailure { e ->
            logger.error("Failed to publish compensation event for step '${SagaStepNames.RECORD_EMAIL_LOG.value}': ${e.message}", e)
        }
    }

    private fun compensateTemplateUpdate(
        sagaId: String,
        step: SagaStep,
    ) {
        runCatching {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val templateName = payload["templateName"]?.toString()

            publishCompensationEvent(
                sagaId = sagaId,
                stepId = step.id,
                action = SagaCompensationActions.LOG_TEMPLATE_COMPENSATION.value,
                extraPayload =
                    mapOf(
                        "templateName" to templateName,
                        "message" to TEMPLATE_UPDATE_COMPENSATION_LOGGED,
                    ),
            )
        }.onFailure { e ->
            logger.error("Failed to publish compensation event for step '${SagaStepNames.TEMPLATE_UPDATE.value}': ${e.message}", e)
        }
    }
}
