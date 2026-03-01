package com.vertyll.veds.iam.infrastructure.kafka

import com.vertyll.veds.iam.domain.model.enums.SagaTypes
import com.vertyll.veds.iam.domain.service.SagaManager
import com.vertyll.veds.sharedinfrastructure.event.mail.MailFailedEvent
import com.vertyll.veds.sharedinfrastructure.event.mail.MailSentEvent
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicsConfig
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

    private companion object {
        /**
         * Saga types where the saga should NOT be completed on mail delivery,
         * because they require an additional user confirmation step.
         */
        val SAGA_TYPES_AWAITING_USER_CONFIRMATION =
            setOf(
                SagaTypes.EMAIL_CHANGE.value,
                SagaTypes.PASSWORD_CHANGE.value,
            )
    }

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

            val saga = sagaManager.findSagaById(sagaId)
            if (saga == null) {
                logger.warn("Saga '{}' not found — skipping MailSentEvent", sagaId)
                return
            }

            if (saga.type in SAGA_TYPES_AWAITING_USER_CONFIRMATION) {
                logger.info(
                    "Mail delivered for saga '{}' (type: {}) — saga remains AWAITING_RESPONSE until user confirms",
                    sagaId,
                    saga.type,
                )
                return
            }

            sagaManager.completeSaga(sagaId)
        } catch (e: Exception) {
            logger.error("Failed to process MailSentEvent: {} — will be retried / sent to DLT", e.message, e)
            throw e
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
            logger.error("Failed to process MailFailedEvent: {} — will be retried / sent to DLT", e.message, e)
            throw e
        }
    }
}
