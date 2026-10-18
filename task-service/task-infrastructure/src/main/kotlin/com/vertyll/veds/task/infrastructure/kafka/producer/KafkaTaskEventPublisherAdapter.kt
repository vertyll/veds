package com.vertyll.veds.task.infrastructure.kafka.producer

import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import com.vertyll.veds.task.TaskArchivedEvent
import com.vertyll.veds.task.TaskAssignedEvent
import com.vertyll.veds.task.TaskCommentAddedEvent
import com.vertyll.veds.task.TaskCreatedEvent
import com.vertyll.veds.task.TaskStatusChangedEvent
import com.vertyll.veds.task.application.port.outbound.TaskEventPublisherPort
import com.vertyll.veds.task.infrastructure.kafka.TaskKafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class KafkaTaskEventPublisherAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : TaskEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publishTaskCreated(
        taskId: UUID,
        projectId: UUID,
        name: String,
        createdBy: UUID,
        assigneeIds: Set<UUID>,
    ) {
        val eventId = Events.newId()
        val event =
            TaskCreatedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTaskId(taskId.toString())
                .setProjectId(projectId.toString())
                .setName(name)
                .setCreatedBy(createdBy.toString())
                .setAssigneeIds(assigneeIds.map { it.toString() })
                .build()
        enqueue(TaskKafkaTopics.TASK_CREATED, taskId, eventId, event)
    }

    override fun publishTaskAssigned(
        taskId: UUID,
        projectId: UUID,
        assigneeIds: Set<UUID>,
        assignedBy: UUID,
    ) {
        val eventId = Events.newId()
        val event =
            TaskAssignedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTaskId(taskId.toString())
                .setProjectId(projectId.toString())
                .setAssigneeIds(assigneeIds.map { it.toString() })
                .setAssignedBy(assignedBy.toString())
                .build()
        enqueue(TaskKafkaTopics.TASK_ASSIGNED, taskId, eventId, event)
    }

    override fun publishTaskStatusChanged(
        taskId: UUID,
        projectId: UUID,
        statusId: UUID?,
        changedBy: UUID,
    ) {
        val eventId = Events.newId()
        val event =
            TaskStatusChangedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTaskId(taskId.toString())
                .setProjectId(projectId.toString())
                .setStatusId(statusId?.toString())
                .setChangedBy(changedBy.toString())
                .build()
        enqueue(TaskKafkaTopics.TASK_STATUS_CHANGED, taskId, eventId, event)
    }

    override fun publishTaskArchived(
        taskId: UUID,
        projectId: UUID,
    ) {
        val eventId = Events.newId()
        val event =
            TaskArchivedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTaskId(taskId.toString())
                .setProjectId(projectId.toString())
                .build()
        enqueue(TaskKafkaTopics.TASK_ARCHIVED, taskId, eventId, event)
    }

    override fun publishCommentAdded(
        taskId: UUID,
        projectId: UUID,
        commentId: UUID,
        authorId: UUID,
        excerpt: String,
    ) {
        val eventId = Events.newId()
        val event =
            TaskCommentAddedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTaskId(taskId.toString())
                .setProjectId(projectId.toString())
                .setCommentId(commentId.toString())
                .setAuthorId(authorId.toString())
                .setExcerpt(excerpt)
                .build()
        enqueue(TaskKafkaTopics.TASK_COMMENT_ADDED, taskId, eventId, event)
    }

    private fun enqueue(
        topic: String,
        taskId: UUID,
        eventId: String,
        event: Any,
    ) {
        val payload = avroPayloadSerializer.serialize(topic, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = topic,
            key = taskId.toString(),
            payload = payload,
            sagaId = null,
            eventId = eventId,
        )
        logger.debug("Saved {} to outbox for task {}", topic, taskId)
    }
}
