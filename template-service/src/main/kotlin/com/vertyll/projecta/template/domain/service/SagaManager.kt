package com.vertyll.projecta.template.domain.service

import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.projecta.sharedinfrastructure.saga.service.BaseSagaManager
import com.vertyll.projecta.template.domain.model.entity.Saga
import com.vertyll.projecta.template.domain.model.entity.SagaStep
import com.vertyll.projecta.template.domain.model.enums.SagaStepNames
import com.vertyll.projecta.template.domain.model.enums.SagaTypes
import com.vertyll.projecta.template.domain.repository.SagaRepository
import com.vertyll.projecta.template.domain.repository.SagaStepRepository
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
    override fun getSagaStepDefinitions(): Map<String, List<String>> =
        mapOf(
            SagaTypes.EXAMPLE_SAGA.value to
                listOf(
                    SagaStepNames.EXAMPLE_STEP.value,
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
            SagaStepNames.EXAMPLE_STEP.value -> logger.info("Compensating example step for saga ${saga.id}")
            else -> logger.warn("No compensation defined for step ${step.stepName}")
        }
    }

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
}
