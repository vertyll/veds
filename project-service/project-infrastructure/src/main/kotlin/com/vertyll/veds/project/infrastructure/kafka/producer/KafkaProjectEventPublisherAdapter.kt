package com.vertyll.veds.project.infrastructure.kafka.producer

import com.vertyll.veds.project.ProjectArchivedEvent
import com.vertyll.veds.project.ProjectCategoryChangedEvent
import com.vertyll.veds.project.ProjectCreatedEvent
import com.vertyll.veds.project.ProjectMemberInvitedEvent
import com.vertyll.veds.project.ProjectMemberJoinedEvent
import com.vertyll.veds.project.ProjectMemberRemovedEvent
import com.vertyll.veds.project.ProjectStatusChangedEvent
import com.vertyll.veds.project.ProjectUpdatedEvent
import com.vertyll.veds.project.application.port.outbound.ProjectEventPublisherPort
import com.vertyll.veds.project.infrastructure.kafka.ProjectKafkaTopics
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Suppress("TooManyFunctions")
internal class KafkaProjectEventPublisherAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : ProjectEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publishProjectCreated(
        projectId: UUID,
        name: String,
        ownerId: UUID,
        hiddenWorkLogEnabled: Boolean,
    ) {
        val eventId = Events.newId()
        val event =
            ProjectCreatedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setName(name)
                .setOwnerId(ownerId.toString())
                .setHiddenWorkLogEnabled(hiddenWorkLogEnabled)
                .build()
        enqueue(ProjectKafkaTopics.PROJECT_CREATED, projectId, eventId, event)
    }

    override fun publishProjectUpdated(
        projectId: UUID,
        name: String,
        hiddenWorkLogEnabled: Boolean,
    ) {
        val eventId = Events.newId()
        val event =
            ProjectUpdatedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setName(name)
                .setHiddenWorkLogEnabled(hiddenWorkLogEnabled)
                .build()
        enqueue(ProjectKafkaTopics.PROJECT_UPDATED, projectId, eventId, event)
    }

    override fun publishProjectArchived(
        projectId: UUID,
        sagaId: String?,
    ) {
        val eventId = Events.newId()
        val event =
            ProjectArchivedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setSagaId(sagaId)
                .build()
        enqueue(ProjectKafkaTopics.PROJECT_ARCHIVED, projectId, eventId, event, sagaId)
    }

    @Suppress("LongParameterList")
    override fun publishMemberInvited(
        projectId: UUID,
        projectName: String,
        invitationId: UUID,
        inviteeEmail: String,
        inviterId: UUID,
        sagaId: String?,
    ) {
        val eventId = Events.newId()
        val event =
            ProjectMemberInvitedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setProjectName(projectName)
                .setInvitationId(invitationId.toString())
                .setInviteeEmail(inviteeEmail)
                .setInviterId(inviterId.toString())
                .setSagaId(sagaId)
                .build()
        enqueue(ProjectKafkaTopics.PROJECT_MEMBER_INVITED, projectId, eventId, event, sagaId)
    }

    override fun publishMemberJoined(
        projectId: UUID,
        memberId: UUID,
        userId: UUID,
        roleCode: String,
    ) {
        val eventId = Events.newId()
        val event =
            ProjectMemberJoinedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setMemberId(memberId.toString())
                .setUserId(userId.toString())
                .setRoleCode(roleCode)
                .build()
        enqueue(ProjectKafkaTopics.PROJECT_MEMBER_JOINED, projectId, eventId, event)
    }

    override fun publishMemberRemoved(
        projectId: UUID,
        userId: UUID,
    ) {
        val eventId = Events.newId()
        val event =
            ProjectMemberRemovedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setUserId(userId.toString())
                .build()
        enqueue(ProjectKafkaTopics.PROJECT_MEMBER_REMOVED, projectId, eventId, event)
    }

    override fun publishCategoryChanged(
        projectId: UUID,
        categoryId: UUID,
        names: Map<String, String>,
        color: String,
        removed: Boolean,
    ) {
        val eventId = Events.newId()
        val event =
            ProjectCategoryChangedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setCategoryId(categoryId.toString())
                .setNames(names)
                .setColor(color)
                .setRemoved(removed)
                .build()
        enqueue(ProjectKafkaTopics.PROJECT_CATEGORY_CHANGED, projectId, eventId, event)
    }

    override fun publishStatusChanged(
        projectId: UUID,
        statusId: UUID,
        names: Map<String, String>,
        color: String,
        removed: Boolean,
    ) {
        val eventId = Events.newId()
        val event =
            ProjectStatusChangedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setStatusId(statusId.toString())
                .setNames(names)
                .setColor(color)
                .setRemoved(removed)
                .build()
        enqueue(ProjectKafkaTopics.PROJECT_STATUS_CHANGED, projectId, eventId, event)
    }

    private fun enqueue(
        topic: String,
        projectId: UUID,
        eventId: String,
        event: Any,
        sagaId: String? = null,
    ) {
        val payload = avroPayloadSerializer.serialize(topic, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = topic,
            key = projectId.toString(),
            payload = payload,
            sagaId = sagaId,
            eventId = eventId,
        )
        logger.debug("Saved {} to outbox for project {} (sagaId: {})", topic, projectId, sagaId)
    }
}
