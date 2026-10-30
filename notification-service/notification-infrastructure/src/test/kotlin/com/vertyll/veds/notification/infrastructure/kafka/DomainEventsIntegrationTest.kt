package com.vertyll.veds.notification.infrastructure.kafka

import com.vertyll.veds.iam.UserRegisteredEvent
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import com.vertyll.veds.notification.domain.repository.RecipientDirectoryRepository
import com.vertyll.veds.notification.infrastructure.IntegrationTestBase
import com.vertyll.veds.project.ProjectMemberInvitedEvent
import com.vertyll.veds.project.ProjectMemberJoinedEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import com.vertyll.veds.task.TaskArchivedEvent
import com.vertyll.veds.task.TaskAssignedEvent
import com.vertyll.veds.task.TaskCreatedEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class DomainEventsIntegrationTest(
    private val kafka: KafkaTemplate<String, ByteArray>,
    private val serializer: AvroPayloadSerializer,
    private val notifications: NotificationRepository,
    private val recipients: RecipientDirectoryRepository,
    private val outbox: OutboxRepository,
) : IntegrationTestBase() {
    private val reader = UUID.randomUUID()
    private val actor = UUID.randomUUID()
    private val projectId = UUID.randomUUID()
    private val taskId = UUID.randomUUID()

    private fun publish(
        topic: String,
        event: Any,
    ) {
        val eventId = Events.newId()
        val record = ProducerRecord<String, ByteArray>(topic, eventId, serializer.serialize(topic, event))
        record.headers().add("eventId", eventId.toByteArray(StandardCharsets.UTF_8))
        kafka.send(record).get()
    }

    private fun typesRaisedFor(recipient: UUID) = notifications.findAllActiveBy(recipient).map { it.type }

    private fun taskAssigned(vararg assignees: UUID) =
        TaskAssignedEvent
            .newBuilder()
            .setEventId(Events.newId())
            .setTimestamp(Events.now())
            .setTaskId(taskId.toString())
            .setProjectId(projectId.toString())
            .setAssigneeIds(assignees.map { it.toString() })
            .setAssignedBy(actor.toString())
            .build()

    private fun taskCreated(vararg assignees: UUID) =
        TaskCreatedEvent
            .newBuilder()
            .setEventId(Events.newId())
            .setTimestamp(Events.now())
            .setTaskId(taskId.toString())
            .setProjectId(projectId.toString())
            .setName("Ship it")
            .setCreatedBy(actor.toString())
            .setAssigneeIds(assignees.map { it.toString() })
            .build()

    @Test
    fun `an assignee hears of a new task and of being assigned`() {
        publish(NotificationKafkaTopics.Consumed.TASK_CREATED, taskCreated(reader))
        publish(NotificationKafkaTopics.Consumed.TASK_ASSIGNED, taskAssigned(reader))

        await().atMost(TIMEOUT).until { typesRaisedFor(reader).size == 2 }
        assertEquals(
            setOf(NotificationType.TASK_CREATED, NotificationType.TASK_ASSIGNED),
            typesRaisedFor(reader).toSet(),
        )
    }

    @Test
    fun `whoever assigned the task is not told about it`() {
        publish(NotificationKafkaTopics.Consumed.TASK_ASSIGNED, taskAssigned(actor, reader))

        await().atMost(TIMEOUT).until { typesRaisedFor(reader).isNotEmpty() }
        assertEquals(emptyList(), typesRaisedFor(actor))
    }

    @Test
    fun `a new member is welcomed`() {
        publish(
            NotificationKafkaTopics.Consumed.PROJECT_MEMBER_JOINED,
            ProjectMemberJoinedEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setMemberId(UUID.randomUUID().toString())
                .setUserId(reader.toString())
                .setRoleCode("MEMBER")
                .build(),
        )

        await().atMost(TIMEOUT).until { typesRaisedFor(reader) == listOf(NotificationType.PROJECT_MEMBER_JOINED) }
    }

    @Test
    fun `an invitation to someone without an account goes out by mail`() {
        val sagaId = "saga-${UUID.randomUUID()}"

        publish(
            NotificationKafkaTopics.Consumed.PROJECT_MEMBER_INVITED,
            ProjectMemberInvitedEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setProjectName("Apollo")
                .setInvitationId(UUID.randomUUID().toString())
                .setInviteeEmail("guest@example.com")
                .setInviterId(actor.toString())
                .setSagaId(sagaId)
                .build(),
        )

        await().atMost(TIMEOUT).until {
            outbox.findAll().any { it.topic == NotificationKafkaTopics.MAIL_REQUESTED && it.sagaId == sagaId }
        }
    }

    @Test
    fun `an archived task takes its notifications with it`() {
        publish(NotificationKafkaTopics.Consumed.TASK_ASSIGNED, taskAssigned(reader))
        await().atMost(TIMEOUT).until { typesRaisedFor(reader).isNotEmpty() }

        publish(
            NotificationKafkaTopics.Consumed.TASK_ARCHIVED,
            TaskArchivedEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setTaskId(taskId.toString())
                .setProjectId(projectId.toString())
                .build(),
        )

        await().atMost(TIMEOUT).until { typesRaisedFor(reader).isEmpty() }
    }

    @Test
    fun `a registered user becomes a known recipient`() {
        publish(
            USER_REGISTERED,
            UserRegisteredEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setUserId(reader.toString())
                .setEmail("reader@example.com")
                .setFirstName("Ada")
                .setLastName("Lovelace")
                .build(),
        )

        await().atMost(TIMEOUT).until { recipients.findById(reader) != null }
        assertTrue(recipients.findByEmail("reader@example.com") != null)
    }

    private companion object {
        const val USER_REGISTERED = "user-registered"
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
