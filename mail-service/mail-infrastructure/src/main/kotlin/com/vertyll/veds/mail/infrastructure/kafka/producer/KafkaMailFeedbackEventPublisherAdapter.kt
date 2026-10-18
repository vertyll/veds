package com.vertyll.veds.mail.infrastructure.kafka.producer

import com.vertyll.veds.mail.application.port.outbound.MailFeedbackEventPublisherPort
import com.vertyll.veds.mail.infrastructure.kafka.MailKafkaTopics
import com.vertyll.veds.mail.mail.MailFailedEvent
import com.vertyll.veds.mail.mail.MailSentEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class KafkaMailFeedbackEventPublisherAdapter(
    private val avroPayloadSerializer: AvroPayloadSerializer,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
) : MailFeedbackEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publishMailSent(
        originSagaId: String,
        to: String,
        subject: String,
        originalEventId: String,
    ) {
        val eventId = Events.newId()
        val event =
            MailSentEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTo(to)
                .setSubject(subject)
                .setOriginalEventId(originalEventId)
                .setSagaId(originSagaId)
                .build()
        val payload = avroPayloadSerializer.serialize(MailKafkaTopics.MAIL_SENT, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = MailKafkaTopics.MAIL_SENT,
            key = originSagaId,
            payload = payload,
            sagaId = originSagaId,
            eventId = eventId,
        )
        logger.debug("Outbox MAIL_SENT for sagaId={}", originSagaId)
    }

    override fun publishMailFailed(
        originSagaId: String,
        to: String,
        subject: String,
        originalEventId: String,
        error: String,
    ) {
        val eventId = Events.newId()
        val event =
            MailFailedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTo(to)
                .setSubject(subject)
                .setOriginalEventId(originalEventId)
                .setError(error)
                .setSagaId(originSagaId)
                .build()
        val payload = avroPayloadSerializer.serialize(MailKafkaTopics.MAIL_FAILED, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = MailKafkaTopics.MAIL_FAILED,
            key = originSagaId,
            payload = payload,
            sagaId = originSagaId,
            eventId = eventId,
        )
        logger.debug("Outbox MAIL_FAILED for sagaId={} error={}", originSagaId, error)
    }
}
