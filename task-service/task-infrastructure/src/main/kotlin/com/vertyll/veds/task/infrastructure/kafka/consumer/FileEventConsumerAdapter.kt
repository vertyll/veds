package com.vertyll.veds.task.infrastructure.kafka.consumer

import com.vertyll.veds.file.FileDeletedEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import com.vertyll.veds.task.application.port.inbound.FileProjectionUseCase
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
internal class FileEventConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val fileProjections: FileProjectionUseCase,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val FILE_DELETED = "file-deleted"
        const val GROUP = "task-service:file-deleted"
    }

    @KafkaListener(topics = [FILE_DELETED])
    @Transactional
    fun onFileDeleted(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (eventId != null && !processedEventGuard.claim(eventId, GROUP)) {
            logger.info("Skipping duplicate event {} for {}", eventId, GROUP)
            return
        }

        try {
            val event = avroPayloadDeserializer.deserialize(FILE_DELETED, payload) as FileDeletedEvent
            fileProjections.fileDeleted(UUID.fromString(event.fileId))
        } catch (e: Exception) {
            logger.error("Failed to apply {}: {} - will be retried / sent to DLT", FILE_DELETED, e.message, e)
            throw e
        }
    }
}
