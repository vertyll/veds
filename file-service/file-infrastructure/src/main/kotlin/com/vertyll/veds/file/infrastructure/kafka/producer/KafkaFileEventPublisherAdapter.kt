package com.vertyll.veds.file.infrastructure.kafka.producer

import com.vertyll.veds.file.FileConfirmedEvent
import com.vertyll.veds.file.FileDeletedEvent
import com.vertyll.veds.file.application.port.outbound.FileEventPublisherPort
import com.vertyll.veds.file.domain.model.FileScope
import com.vertyll.veds.file.infrastructure.kafka.FileKafkaTopics
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class KafkaFileEventPublisherAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : FileEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publishFileConfirmed(
        fileId: UUID,
        scope: FileScope,
        scopeId: UUID?,
        ownerId: UUID,
    ) {
        val eventId = Events.newId()
        val event =
            FileConfirmedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setFileId(fileId.toString())
                .setScope(scope.name)
                .setScopeId(scopeId?.toString())
                .setOwnerId(ownerId.toString())
                .build()
        enqueue(FileKafkaTopics.FILE_CONFIRMED, fileId, eventId, event)
    }

    override fun publishFileDeleted(
        fileId: UUID,
        scope: FileScope,
        scopeId: UUID?,
    ) {
        val eventId = Events.newId()
        val event =
            FileDeletedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setFileId(fileId.toString())
                .setScope(scope.name)
                .setScopeId(scopeId?.toString())
                .build()
        enqueue(FileKafkaTopics.FILE_DELETED, fileId, eventId, event)
    }

    private fun enqueue(
        topic: String,
        fileId: UUID,
        eventId: String,
        event: Any,
    ) {
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = topic,
            key = fileId.toString(),
            payload = avroPayloadSerializer.serialize(topic, event),
            sagaId = null,
            eventId = eventId,
        )
        logger.debug("Saved {} to outbox for file {}", topic, fileId)
    }
}
