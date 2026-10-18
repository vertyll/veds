package com.vertyll.veds.iam.infrastructure.kafka.producer

import com.vertyll.veds.iam.UserProfileUpdatedEvent
import com.vertyll.veds.iam.UserRegisteredEvent
import com.vertyll.veds.iam.application.port.outbound.AuthEventPublisherPort
import com.vertyll.veds.iam.infrastructure.kafka.IamKafkaTopics
import com.vertyll.veds.mail.mail.MailRequestedCommand
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class KafkaAuthEventPublisherAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : AuthEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun requestMail(
        to: String,
        templateName: String,
        variables: Map<String, String>,
        replyTo: String?,
        priority: Int,
        sagaId: String?,
    ) {
        val eventId = Events.newId()
        val event =
            MailRequestedCommand
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTo(to)
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

    override fun publishUserRegistered(
        userId: UUID,
        email: String,
        firstName: String?,
        lastName: String?,
    ) {
        val eventId = Events.newId()
        val event =
            UserRegisteredEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setUserId(userId.toString())
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .build()
        enqueueUserEvent(IamKafkaTopics.USER_REGISTERED, userId, eventId, event)
    }

    override fun publishUserProfileUpdated(
        userId: UUID,
        email: String,
        firstName: String?,
        lastName: String?,
        avatarFileId: UUID?,
    ) {
        val eventId = Events.newId()
        val event =
            UserProfileUpdatedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setUserId(userId.toString())
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setAvatarFileId(avatarFileId?.toString())
                .build()
        enqueueUserEvent(IamKafkaTopics.USER_PROFILE_UPDATED, userId, eventId, event)
    }

    private fun enqueueUserEvent(
        topic: String,
        userId: UUID,
        eventId: String,
        event: Any,
    ) {
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = topic,
            key = userId.toString(),
            payload = avroPayloadSerializer.serialize(topic, event),
            sagaId = null,
            eventId = eventId,
        )
        logger.debug("Saved {} to outbox for user {}", topic, userId)
    }
}
