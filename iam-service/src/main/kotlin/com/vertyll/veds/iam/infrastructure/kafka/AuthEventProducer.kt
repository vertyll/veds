package com.vertyll.veds.iam.infrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.event.mail.MailRequestedEvent
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicNames
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AuthEventProducer(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendMailRequestedEvent(event: MailRequestedEvent) {
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = KafkaTopicNames.MAIL_REQUESTED,
            key = event.eventId,
            payload = event,
            sagaId = event.sagaId,
            eventId = event.eventId,
        )
        logger.info("Saved mail request to outbox for: ${event.to} (sagaId: ${event.sagaId})")
    }
}
