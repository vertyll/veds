package com.vertyll.veds.template.domain.service

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.BaseSagaManager
import com.vertyll.veds.template.domain.model.entity.Saga
import com.vertyll.veds.template.domain.model.entity.SagaStep
import com.vertyll.veds.template.domain.model.enums.SagaStepNames
import com.vertyll.veds.template.domain.repository.SagaRepository
import com.vertyll.veds.template.domain.repository.SagaStepRepository
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
            SagaStepNames.EXAMPLE_STEP.value -> logger.info("Compensating example step for saga ${saga.id}")
            else -> logger.warn("No compensation defined for step ${step.stepName}")
        }
    }
}