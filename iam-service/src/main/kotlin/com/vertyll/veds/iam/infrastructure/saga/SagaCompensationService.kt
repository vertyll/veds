package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.infrastructure.config.SagaConfig
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationEngine
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

/**
 * Thin Kafka adapter for choreography saga compensations targeted at iam-service.
 *
 * Delegates all logic to the shared [SagaCompensationEngine] (composition over
 * inheritance). Dispatch on the compensation `action` is performed by
 * `IamSagaCompensationHandler` injected into the engine via [SagaConfig].
 */
@Service
internal class SagaCompensationService(
    private val sagaCompensationEngine: SagaCompensationEngine<SagaStepJpaEntity>,
) {
    @KafkaListener(topics = [SagaConfig.SAGA_COMPENSATION_TOPIC])
    fun handleCompensationEvent(payload: String) = sagaCompensationEngine.handleCompensationEvent(payload)
}
