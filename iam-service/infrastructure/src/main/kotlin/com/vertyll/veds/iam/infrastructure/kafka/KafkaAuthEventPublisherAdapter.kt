package com.vertyll.veds.iam.infrastructure.kafka

import com.vertyll.veds.iam.application.port.out.AuthEventPublisherPort
import com.vertyll.veds.iam.mail.MailRequestedEvent
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.event.Events
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KafkaAuthEventPublisherAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : AuthEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun sendMailRequestedEvent(
        to: String,
        subject: String,
        templateName: String,
        variables: Map<String, String>,
        replyTo: String?,
        priority: Int,
        sagaId: String?,
    ) {
        val eventId = Events.newId()
        val event =
            MailRequestedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTo(to)
                .setSubject(subject)
                .setTemplateName(templateName)
                .setVariables(variables)
                .setReplyTo(replyTo)
                .setPriority(priority)
                .setSagaId(sagaId)
                .build()
        val payload = avroPayloadSerializer.serialize(IamKafkaTopics.MAIL_REQUESTED, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = IamKafkaTopics.MAIL_REQUESTED,
            key = eventId,
            payload = payload,
            sagaId = sagaId,
            eventId = eventId,
        )
        logger.info("Saved mail request to outbox for: $to (sagaId: $sagaId)")
    }
}
