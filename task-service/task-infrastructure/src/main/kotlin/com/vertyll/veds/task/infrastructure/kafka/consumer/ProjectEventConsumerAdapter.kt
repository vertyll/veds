package com.vertyll.veds.task.infrastructure.kafka.consumer

import com.vertyll.veds.project.ProjectArchivedEvent
import com.vertyll.veds.project.ProjectCategoryChangedEvent
import com.vertyll.veds.project.ProjectCreatedEvent
import com.vertyll.veds.project.ProjectMemberJoinedEvent
import com.vertyll.veds.project.ProjectMemberRemovedEvent
import com.vertyll.veds.project.ProjectStatusChangedEvent
import com.vertyll.veds.project.ProjectUpdatedEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import com.vertyll.veds.task.application.port.inbound.ProjectProjectionUseCase
import com.vertyll.veds.task.domain.model.ProjectCategoryRef
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.ProjectStatusRef
import com.vertyll.veds.task.infrastructure.kafka.TaskKafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Suppress("TooManyFunctions")
internal class ProjectEventConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val projections: ProjectProjectionUseCase,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val GROUP_PREFIX = "task-service:"
    }

    @KafkaListener(topics = [TaskKafkaTopics.Consumed.PROJECT_CREATED])
    @Transactional
    fun onProjectCreated(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, TaskKafkaTopics.Consumed.PROJECT_CREATED) {
        val event = decode(TaskKafkaTopics.Consumed.PROJECT_CREATED, payload) as ProjectCreatedEvent
        projections.projectChanged(
            ProjectRef(
                projectId = UUID.fromString(event.projectId),
                name = event.name.toString(),
                hiddenWorkLogEnabled = event.hiddenWorkLogEnabled,
            ),
        )
    }

    @KafkaListener(topics = [TaskKafkaTopics.Consumed.PROJECT_UPDATED])
    @Transactional
    fun onProjectUpdated(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, TaskKafkaTopics.Consumed.PROJECT_UPDATED) {
        val event = decode(TaskKafkaTopics.Consumed.PROJECT_UPDATED, payload) as ProjectUpdatedEvent
        projections.projectChanged(
            ProjectRef(
                projectId = UUID.fromString(event.projectId),
                name = event.name.toString(),
                hiddenWorkLogEnabled = event.hiddenWorkLogEnabled,
            ),
        )
    }

    @KafkaListener(topics = [TaskKafkaTopics.Consumed.PROJECT_ARCHIVED])
    @Transactional
    fun onProjectArchived(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, TaskKafkaTopics.Consumed.PROJECT_ARCHIVED) {
        val event = decode(TaskKafkaTopics.Consumed.PROJECT_ARCHIVED, payload) as ProjectArchivedEvent
        projections.projectArchived(UUID.fromString(event.projectId))
    }

    @KafkaListener(topics = [TaskKafkaTopics.Consumed.PROJECT_CATEGORY_CHANGED])
    @Transactional
    fun onCategoryChanged(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, TaskKafkaTopics.Consumed.PROJECT_CATEGORY_CHANGED) {
        val event =
            decode(TaskKafkaTopics.Consumed.PROJECT_CATEGORY_CHANGED, payload) as ProjectCategoryChangedEvent
        val categoryId = UUID.fromString(event.categoryId)

        if (event.removed) {
            projections.categoryRemoved(categoryId)
        } else {
            projections.categoryChanged(
                ProjectCategoryRef(
                    categoryId = categoryId,
                    projectId = UUID.fromString(event.projectId),
                    names = event.names.entries.associate { it.key.toString() to it.value.toString() },
                    color = event.color,
                ),
            )
        }
    }

    @KafkaListener(topics = [TaskKafkaTopics.Consumed.PROJECT_STATUS_CHANGED])
    @Transactional
    fun onStatusChanged(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, TaskKafkaTopics.Consumed.PROJECT_STATUS_CHANGED) {
        val event = decode(TaskKafkaTopics.Consumed.PROJECT_STATUS_CHANGED, payload) as ProjectStatusChangedEvent
        val statusId = UUID.fromString(event.statusId)

        if (event.removed) {
            projections.statusRemoved(statusId)
        } else {
            projections.statusChanged(
                ProjectStatusRef(
                    statusId = statusId,
                    projectId = UUID.fromString(event.projectId),
                    names = event.names.entries.associate { it.key.toString() to it.value.toString() },
                    color = event.color,
                ),
            )
        }
    }

    @KafkaListener(topics = [TaskKafkaTopics.Consumed.PROJECT_MEMBER_JOINED])
    @Transactional
    fun onMemberJoined(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, TaskKafkaTopics.Consumed.PROJECT_MEMBER_JOINED) {
        val event = decode(TaskKafkaTopics.Consumed.PROJECT_MEMBER_JOINED, payload) as ProjectMemberJoinedEvent
        projections.memberJoined(
            ProjectMembershipRef(
                projectId = UUID.fromString(event.projectId),
                userId = UUID.fromString(event.userId),
                roleCode = event.roleCode,
            ),
        )
    }

    @KafkaListener(topics = [TaskKafkaTopics.Consumed.PROJECT_MEMBER_REMOVED])
    @Transactional
    fun onMemberRemoved(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, TaskKafkaTopics.Consumed.PROJECT_MEMBER_REMOVED) {
        val event = decode(TaskKafkaTopics.Consumed.PROJECT_MEMBER_REMOVED, payload) as ProjectMemberRemovedEvent
        projections.memberRemoved(
            projectId = UUID.fromString(event.projectId),
            userId = UUID.fromString(event.userId),
        )
    }

    private fun decode(
        topic: String,
        payload: ByteArray,
    ): Any = avroPayloadDeserializer.deserialize(topic, payload)

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
