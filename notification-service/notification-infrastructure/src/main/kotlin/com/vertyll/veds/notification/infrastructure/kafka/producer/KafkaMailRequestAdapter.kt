package com.vertyll.veds.notification.infrastructure.kafka.producer

import com.vertyll.veds.mail.mail.MailRequestedCommand
import com.vertyll.veds.notification.application.port.outbound.MailRequestPort
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.infrastructure.kafka.NotificationKafkaTopics
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class KafkaMailRequestAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : MailRequestPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun requestMail(
        to: String,
        type: NotificationType,
        params: Map<String, String>,
        originSagaId: String?,
    ) {
        val eventId = Events.newId()
        val event =
            MailRequestedCommand
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTo(to)
                .setTemplateName(type.name)
                .setVariables(params)
                .setReplyTo(null)
                .setPriority(0)
                .setSagaId(originSagaId)
                .build()

        val payload = avroPayloadSerializer.serialize(NotificationKafkaTopics.MAIL_REQUESTED, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = NotificationKafkaTopics.MAIL_REQUESTED,
            key = to,
            payload = payload,
            sagaId = originSagaId,
            eventId = eventId,
        )
        logger.debug("Queued notification e-mail to {} for {}", to, type)
    }
}
