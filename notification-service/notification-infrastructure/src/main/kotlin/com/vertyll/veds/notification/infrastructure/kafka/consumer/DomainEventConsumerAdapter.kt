package com.vertyll.veds.notification.infrastructure.kafka.consumer

import com.vertyll.veds.notification.application.command.RaiseNotificationCommand
import com.vertyll.veds.notification.application.command.RetireNotificationsCommand
import com.vertyll.veds.notification.application.port.inbound.command.NotificationCommandUseCase
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.infrastructure.kafka.NotificationKafkaTopics
import com.vertyll.veds.project.ProjectMemberInvitedEvent
import com.vertyll.veds.project.ProjectMemberJoinedEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import com.vertyll.veds.task.TaskArchivedEvent
import com.vertyll.veds.task.TaskAssignedEvent
import com.vertyll.veds.task.TaskCommentAddedEvent
import com.vertyll.veds.task.TaskCreatedEvent
import com.vertyll.veds.task.TaskStatusChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Suppress("TooManyFunctions")
internal class DomainEventConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val notifications: NotificationCommandUseCase,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val GROUP_PREFIX = "notification-service:"
        private const val PARAM_PROJECT = "projectName"
        private const val PARAM_TASK = "taskName"
        private const val PARAM_ACTOR = "actorId"
        private const val PARAM_EXCERPT = "excerpt"
    }

    @KafkaListener(topics = [NotificationKafkaTopics.Consumed.PROJECT_MEMBER_INVITED])
    @Transactional
    fun onMemberInvited(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, NotificationKafkaTopics.Consumed.PROJECT_MEMBER_INVITED) {
        val event = decode(NotificationKafkaTopics.Consumed.PROJECT_MEMBER_INVITED, payload) as ProjectMemberInvitedEvent

        notifications.raise(
            RaiseNotificationCommand(
                recipientIds = emptySet(),
                type = NotificationType.PROJECT_INVITATION,
                params = mapOf(PARAM_PROJECT to event.projectName.toString()),
                projectId = UUID.fromString(event.projectId),
                subjectId = UUID.fromString(event.invitationId),
                fallbackEmail = event.inviteeEmail,
                originSagaId = event.sagaId,
            ),
        )
    }

    @KafkaListener(topics = [NotificationKafkaTopics.Consumed.PROJECT_MEMBER_JOINED])
    @Transactional
    fun onMemberJoined(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, NotificationKafkaTopics.Consumed.PROJECT_MEMBER_JOINED) {
        val event = decode(NotificationKafkaTopics.Consumed.PROJECT_MEMBER_JOINED, payload) as ProjectMemberJoinedEvent
        val userId = UUID.fromString(event.userId)

        notifications.raise(
            RaiseNotificationCommand(
                recipientIds = setOf(userId),
                type = NotificationType.PROJECT_MEMBER_JOINED,
                params = mapOf("roleCode" to event.roleCode.toString()),
                projectId = UUID.fromString(event.projectId),
                subjectId = UUID.fromString(event.memberId),
            ),
        )
    }

    @KafkaListener(topics = [NotificationKafkaTopics.Consumed.TASK_CREATED])
    @Transactional
    fun onTaskCreated(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, NotificationKafkaTopics.Consumed.TASK_CREATED) {
        val event = decode(NotificationKafkaTopics.Consumed.TASK_CREATED, payload) as TaskCreatedEvent
        val creator = UUID.fromString(event.createdBy)

        notifications.raise(
            RaiseNotificationCommand(
                recipientIds = event.assigneeIds.map { UUID.fromString(it.toString()) }.toSet(),
                type = NotificationType.TASK_CREATED,
                params = mapOf(PARAM_TASK to event.name, PARAM_ACTOR to creator.toString()),
                projectId = UUID.fromString(event.projectId),
                subjectId = UUID.fromString(event.taskId),
                excludeUserId = creator,
            ),
        )
    }

    @KafkaListener(topics = [NotificationKafkaTopics.Consumed.TASK_ASSIGNED])
    @Transactional
    fun onTaskAssigned(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, NotificationKafkaTopics.Consumed.TASK_ASSIGNED) {
        val event = decode(NotificationKafkaTopics.Consumed.TASK_ASSIGNED, payload) as TaskAssignedEvent
        val actor = UUID.fromString(event.assignedBy)

        notifications.raise(
            RaiseNotificationCommand(
                recipientIds = event.assigneeIds.map { UUID.fromString(it.toString()) }.toSet(),
                type = NotificationType.TASK_ASSIGNED,
                params = mapOf(PARAM_ACTOR to actor.toString()),
                projectId = UUID.fromString(event.projectId),
                subjectId = UUID.fromString(event.taskId),
                excludeUserId = actor,
            ),
        )
    }

    @KafkaListener(topics = [NotificationKafkaTopics.Consumed.TASK_STATUS_CHANGED])
    @Transactional
    fun onTaskStatusChanged(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, NotificationKafkaTopics.Consumed.TASK_STATUS_CHANGED) {
        val event = decode(NotificationKafkaTopics.Consumed.TASK_STATUS_CHANGED, payload) as TaskStatusChangedEvent
        val actor = UUID.fromString(event.changedBy)

        notifications.raise(
            RaiseNotificationCommand(
                recipientIds = emptySet(),
                type = NotificationType.TASK_STATUS_CHANGED,
                params = mapOf(PARAM_ACTOR to actor.toString()),
                projectId = UUID.fromString(event.projectId),
                subjectId = UUID.fromString(event.taskId),
                excludeUserId = actor,
            ),
        )
    }

    @KafkaListener(topics = [NotificationKafkaTopics.Consumed.TASK_COMMENT_ADDED])
    @Transactional
    fun onCommentAdded(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, NotificationKafkaTopics.Consumed.TASK_COMMENT_ADDED) {
        val event = decode(NotificationKafkaTopics.Consumed.TASK_COMMENT_ADDED, payload) as TaskCommentAddedEvent
        val author = UUID.fromString(event.authorId)

        notifications.raise(
            RaiseNotificationCommand(
                recipientIds = emptySet(),
                type = NotificationType.TASK_COMMENT_ADDED,
                params = mapOf(PARAM_EXCERPT to event.excerpt, PARAM_ACTOR to author.toString()),
                projectId = UUID.fromString(event.projectId),
                subjectId = UUID.fromString(event.taskId),
                excludeUserId = author,
            ),
        )
    }

    @KafkaListener(topics = [NotificationKafkaTopics.Consumed.TASK_ARCHIVED])
    @Transactional
    fun onTaskArchived(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) = consume(eventId, NotificationKafkaTopics.Consumed.TASK_ARCHIVED) {
        val event = decode(NotificationKafkaTopics.Consumed.TASK_ARCHIVED, payload) as TaskArchivedEvent
        notifications.retire(RetireNotificationsCommand(UUID.fromString(event.taskId)))
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
            logger.error("Failed to handle {}: {} - will be retried / sent to DLT", topic, e.message, e)
            throw e
        }
    }
}
