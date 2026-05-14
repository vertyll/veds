package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.BaseSagaManager
import com.vertyll.veds.template.application.saga.model.Saga
import com.vertyll.veds.template.application.saga.model.SagaCompensationActions
import com.vertyll.veds.template.application.saga.model.SagaStepNames
import com.vertyll.veds.template.application.saga.model.SagaTypes
import com.vertyll.veds.template.application.saga.port.SagaProcessPort
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.template.infrastructure.persistence.repository.SagaStepJpaRepository
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
        const val SAGA_COMPENSATION_TOPIC = "saga-compensation-template"
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
                SagaStepNames.PERSIST_TEMPLATE.value -> {
                    val p = step.payload?.let { objectMapper.readValue(it, Map::class.java) } ?: emptyMap<String, Any>()
                    publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.DELETE_TEMPLATE.value,
                        mapOf("templateId" to p["templateId"]),
                    )
                }
                SagaStepNames.PUBLISH_TEMPLATE_EVENT.value -> {
                    val p = step.payload?.let { objectMapper.readValue(it, Map::class.java) } ?: emptyMap<String, Any>()
                    publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.LOG_TEMPLATE_COMPENSATION.value,
                        mapOf("templateId" to p["templateId"]),
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
