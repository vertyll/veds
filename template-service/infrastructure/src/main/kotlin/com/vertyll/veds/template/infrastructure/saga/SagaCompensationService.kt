package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.kafka.ProcessedEventGuard
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationEngine
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.infrastructure.config.SagaConfig
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaStepJpaEntity
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

/**
 * Thin Kafka adapter for choreography saga compensations targeted at
 * template-service.
 *
 * Delegates business logic to [SagaCompensationEngine]; protects the
 * engine from duplicate events via [ProcessedEventGuard].
 *
 * Exceptions from the engine are intentionally propagated to Spring
 * Kafka's `DefaultErrorHandler` so failures land in DLT after the
 * configured retry budget. The `SagaWatchdog` still provides a slower
 * cooldown-based safety net for sagas stuck in `COMPENSATING` /
 * `COMPENSATION_FAILED`.
 */
@Service
internal class SagaCompensationService(
    private val sagaCompensationEngine: SagaCompensationEngine<SagaStepJpaEntity, TemplateCompensationCommand>,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CONSUMER_GROUP = "template-service:saga-compensation"
    }

    @KafkaListener(topics = [SagaConfig.SAGA_COMPENSATION_TOPIC])
    fun handleCompensationEvent(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (eventId != null && !processedEventGuard.claim(eventId, CONSUMER_GROUP)) {
            logger.info("Skipping duplicate compensation event: eventId={}", eventId)
            return
        }
        sagaCompensationEngine.handleCompensationEvent(payload)
    }
}
