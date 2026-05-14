package com.vertyll.veds.mail.infrastructure.saga

import com.vertyll.veds.mail.application.saga.model.Saga
import com.vertyll.veds.mail.application.saga.model.SagaCompensationActions
import com.vertyll.veds.mail.application.saga.model.SagaStepNames
import com.vertyll.veds.mail.application.saga.model.SagaTypes
import com.vertyll.veds.mail.application.saga.port.SagaProcessPort
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.BaseSagaManager
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
internal class SagaManagerAdapter(
    sagaRepository: SagaJpaRepository,
    sagaStepRepository: SagaStepJpaRepository,
    kafkaOutboxProcessor: KafkaOutboxProcessor,
    objectMapper: ObjectMapper,
) : BaseSagaManager<SagaJpaEntity, SagaStepJpaEntity>(
        sagaRepository,
        sagaStepRepository,
        kafkaOutboxProcessor,
        objectMapper,
    ),
    SagaProcessPort {
    override val compensationTopic = SAGA_COMPENSATION_TOPIC

    companion object {
        const val SAGA_COMPENSATION_TOPIC = "saga-compensation-mail"
    }

    override fun beginSaga(
        sagaType: SagaTypes,
        payload: Map<String, Any?>,
    ): Saga = super.startSaga(sagaType = sagaType, payload = payload).toSagaDomain()

    override fun appendSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Map<String, Any?>,
    ) {
        super.recordSagaStep(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            payload = payload,
        )
    }

    override fun markSagaCompleted(sagaId: String) {
        super.completeSaga(sagaId)
    }

    override fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    ) {
        super.failSaga(sagaId, errorMessage)
    }

    override fun markAwaitingResponse(sagaId: String) {
        super.awaitResponse(sagaId)
    }

    override fun findSagaDomainById(sagaId: String): Saga? = super.findSagaById(sagaId)?.toSagaDomain()

    override fun createSagaEntity(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ): SagaJpaEntity =
        SagaJpaEntity(
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
    ): SagaStepJpaEntity =
        SagaStepJpaEntity(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            payload = payload,
            createdAt = createdAt,
        )

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
    ) {
        try {
            when (step.stepName) {
                SagaStepNames.SEND_EMAIL.value -> {
                    val p = step.payload?.let { objectMapper.readValue(it, Map::class.java) } ?: emptyMap<String, Any>()
                    publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.LOG_EMAIL_COMPENSATION.value,
                        mapOf(
                            "emailId" to p["emailId"],
                            "to" to p["to"],
                            "message" to "Email cannot be unsent",
                        ),
                    )
                }
                SagaStepNames.RECORD_EMAIL_LOG.value -> {
                    val p = step.payload?.let { objectMapper.readValue(it, Map::class.java) } ?: emptyMap<String, Any>()
                    publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.DELETE_EMAIL_LOG.value,
                        mapOf("logId" to p["logId"]),
                    )
                }
                SagaStepNames.TEMPLATE_UPDATE.value -> {
                    val p = step.payload?.let { objectMapper.readValue(it, Map::class.java) } ?: emptyMap<String, Any>()
                    publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.LOG_TEMPLATE_COMPENSATION.value,
                        mapOf("templateName" to p["templateName"]),
                    )
                }
                else -> logger.warn("No compensation defined for step '${step.stepName}'")
            }
        } catch (e: Exception) {
            logger.error("Failed compensation for step ${step.stepName}", e)
        }
    }
}

private fun SagaJpaEntity.toSagaDomain(): Saga =
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
