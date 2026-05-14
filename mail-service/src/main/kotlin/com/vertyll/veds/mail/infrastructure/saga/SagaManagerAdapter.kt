package com.vertyll.veds.mail.infrastructure.saga

import com.vertyll.veds.mail.application.saga.port.SagaProcessPort
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.mail.infrastructure.persistence.repository.SagaStepJpaRepository
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue
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

    override fun startSaga(
        sagaType: String,
        payload: Map<String, Any?>,
    ): String =
        super
            .startSaga(
                sagaType =
                    object : SagaTypeValue {
                        override val value = sagaType
                    },
                payload = payload,
            ).id

    override fun recordSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: Map<String, Any?>,
    ) {
        super.recordSagaStep(
            sagaId = sagaId,
            stepName =
                object : SagaTypeValue {
                    override val value = stepName
                },
            status = status,
            payload = payload,
        )
    }

    override fun markSagaCompleted(sagaId: String) {
        super.completeSaga(sagaId)
    }

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
                "SEND_EMAIL" -> {
                    val p = step.payload?.let { objectMapper.readValue(it, Map::class.java) } ?: emptyMap<String, Any>()
                    publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        "LOG_EMAIL_COMPENSATION",
                        mapOf(
                            "emailId" to p["emailId"],
                            "to" to p["to"],
                            "message" to "Email cannot be unsent",
                        ),
                    )
                }
                "RECORD_EMAIL_LOG" -> {
                    val p = step.payload?.let { objectMapper.readValue(it, Map::class.java) } ?: emptyMap<String, Any>()
                    publishCompensationEvent(saga.id, step.id ?: 0L, "DELETE_EMAIL_LOG", mapOf("logId" to p["logId"]))
                }
                "TEMPLATE_UPDATE" -> {
                    val p = step.payload?.let { objectMapper.readValue(it, Map::class.java) } ?: emptyMap<String, Any>()
                    publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        "LOG_TEMPLATE_COMPENSATION",
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
