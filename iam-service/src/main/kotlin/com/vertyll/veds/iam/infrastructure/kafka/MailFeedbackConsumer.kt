package com.vertyll.veds.iam.infrastructure.kafka

import com.vertyll.veds.iam.domain.model.enums.SagaStepNames
import com.vertyll.veds.iam.domain.service.SagaManager
import com.vertyll.veds.sharedinfrastructure.event.mail.MailFailedEvent
import com.vertyll.veds.sharedinfrastructure.event.mail.MailSentEvent
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicsConfig
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class MailFeedbackConsumer(
    private val objectMapper: ObjectMapper,
    private val sagaManager: SagaManager,
    @Suppress("unused") private val kafkaTopicsConfig: KafkaTopicsConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getMailSentTopic()}"])
    fun handleMailSent(
        @Payload payload: String,
    ) {
        try {
            val event = objectMapper.readValue<MailSentEvent>(payload)
            val sagaId = event.sagaId

            if (sagaId == null) {
                logger.debug("Received MailSentEvent without sagaId — skipping saga step recording")
                return
            }

            logger.info("Received MailSentEvent for saga: {} (to: {})", sagaId, event.to)

            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.MAIL_DELIVERED,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "to" to event.to,
                        "subject" to event.subject,
                        "originalEventId" to event.originalEventId,
                    ),
            )
        } catch (e: Exception) {
            logger.error("Failed to process MailSentEvent: {}", e.message, e)
        }
    }

    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getMailFailedTopic()}"])
    fun handleMailFailed(
        @Payload payload: String,
    ) {
        try {
            val event = objectMapper.readValue<MailFailedEvent>(payload)
            val sagaId = event.sagaId

            if (sagaId == null) {
                logger.debug("Received MailFailedEvent without sagaId — skipping saga failure")
                return
            }

            logger.warn("Received MailFailedEvent for saga: {} (to: {}, error: {})", sagaId, event.to, event.error)

            sagaManager.failSaga(sagaId, "Mail delivery failed: ${event.error}")
        } catch (e: Exception) {
            logger.error("Failed to process MailFailedEvent: {}", e.message, e)
        }
    }
}
