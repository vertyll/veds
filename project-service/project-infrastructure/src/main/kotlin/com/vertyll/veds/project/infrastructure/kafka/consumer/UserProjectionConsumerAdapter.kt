package com.vertyll.veds.project.infrastructure.kafka.consumer

import com.vertyll.veds.iam.UserProfileUpdatedEvent
import com.vertyll.veds.iam.UserRegisteredEvent
import com.vertyll.veds.project.domain.model.UserRef
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
internal class UserProjectionConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val directory: UserDirectoryRepository,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val USER_REGISTERED = "user-registered"
        const val USER_PROFILE_UPDATED = "user-profile-updated"
        const val GROUP_PREFIX = "project-service:"
    }

    @KafkaListener(topics = [USER_REGISTERED])
    @Transactional
    fun onUserRegistered(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, USER_REGISTERED) {
        val event = avroPayloadDeserializer.deserialize(USER_REGISTERED, payload) as UserRegisteredEvent
        directory.save(
            UserRef(
                userId = UUID.fromString(event.userId),
                email = event.email,
                firstName = event.firstName,
                lastName = event.lastName,
            ),
        )
    }

    @KafkaListener(topics = [USER_PROFILE_UPDATED])
    @Transactional
    fun onUserProfileUpdated(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, USER_PROFILE_UPDATED) {
        val event =
            avroPayloadDeserializer.deserialize(USER_PROFILE_UPDATED, payload) as UserProfileUpdatedEvent
        directory.save(
            UserRef(
                userId = UUID.fromString(event.userId),
                email = event.email,
                firstName = event.firstName,
                lastName = event.lastName,
                avatarFileId = event.avatarFileId?.let(UUID::fromString),
            ),
        )
    }

    private fun consume(
        eventId: String?,
        topic: String,
        block: () -> Unit,
    ) {
        val group = "$GROUP_PREFIX$topic"
        if (eventId != null && !processedEventGuard.claim(eventId, group)) {
            logger.info("Skipping duplicate event {} for {}", eventId, group)
            return
        }
        try {
            block()
        } catch (e: Exception) {
            logger.error("Failed to project {}: {} - will be retried / sent to DLT", topic, e.message, e)
            throw e
        }
    }
}
