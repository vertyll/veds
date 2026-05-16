package com.vertyll.veds.mail.infrastructure.kafka.producer

import com.vertyll.veds.mail.application.port.out.MailFeedbackEventPublisherPort
import com.vertyll.veds.mail.infrastructure.kafka.MailKafkaTopics
import com.vertyll.veds.mail.mail.MailFailedEvent
import com.vertyll.veds.mail.mail.MailSentEvent
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.event.Events
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Outbound Kafka + Avro adapter for the mail-feedback choreography events
 * (`mail-sent` / `mail-failed`). Uses the transactional outbox to keep
 * publishing reliable inside the same DB transaction as the domain change.
 *
 * This is the only place in the service that knows about Avro generated
 * `SpecificRecord` classes, Schema Registry framing and topic names.
 */
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
