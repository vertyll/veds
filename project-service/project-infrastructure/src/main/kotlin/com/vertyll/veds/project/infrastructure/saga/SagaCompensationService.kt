package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand
import com.vertyll.veds.project.infrastructure.config.SagaConfig
import com.vertyll.veds.project.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import com.vertyll.veds.shared.saga.engine.SagaCompensationEngine
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class SagaCompensationService(
    private val sagaCompensationEngine: SagaCompensationEngine<SagaStepJpaEntity, ProjectCompensationCommand>,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CONSUMER_GROUP = "project-service:saga-compensation"
    }

    @KafkaListener(topics = [SagaConfig.SAGA_COMPENSATION_TOPIC])
    @Transactional
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
