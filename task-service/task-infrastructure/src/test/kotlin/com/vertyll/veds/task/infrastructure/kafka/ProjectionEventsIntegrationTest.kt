@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.infrastructure.kafka

import com.vertyll.veds.iam.RolePermissionsChangedEvent
import com.vertyll.veds.iam.UserProfileUpdatedEvent
import com.vertyll.veds.iam.UserRegisteredEvent
import com.vertyll.veds.project.ProjectArchivedEvent
import com.vertyll.veds.project.ProjectCategoryChangedEvent
import com.vertyll.veds.project.ProjectCreatedEvent
import com.vertyll.veds.project.ProjectMemberJoinedEvent
import com.vertyll.veds.project.ProjectMemberRemovedEvent
import com.vertyll.veds.project.ProjectStatusChangedEvent
import com.vertyll.veds.project.ProjectUpdatedEvent
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.domain.repository.RolePermissionsRepository
import com.vertyll.veds.task.domain.repository.UserDirectoryRepository
import com.vertyll.veds.task.infrastructure.IntegrationTestBase
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectionEventsIntegrationTest(
    private val kafka: KafkaTemplate<String, ByteArray>,
    private val serializer: AvroPayloadSerializer,
    private val directory: ProjectDirectoryRepository,
    private val users: UserDirectoryRepository,
    private val roles: RolePermissionsRepository,
) : IntegrationTestBase() {
    private val projectId: UUID = Uuid.generateV7().toJavaUuid()
    private val userId: UUID = Uuid.generateV7().toJavaUuid()

    private fun publish(
        topic: String,
        event: Any,
        eventId: String = Events.newId(),
    ) {
        val record = ProducerRecord<String, ByteArray>(topic, eventId, serializer.serialize(topic, event))
        record.headers().add("eventId", eventId.toByteArray(StandardCharsets.UTF_8))
        kafka.send(record).get()
    }

    private fun projectCreated(name: String = "Apollo") =
        ProjectCreatedEvent
            .newBuilder()
            .setEventId(Events.newId())
            .setTimestamp(Events.now())
            .setProjectId(projectId.toString())
            .setName(name)
            .setOwnerId(userId.toString())
            .setHiddenWorkLogEnabled(false)
            .build()

    private fun awaitProjectNamed(name: String) = await().atMost(TIMEOUT).until { directory.findProject(projectId)?.name == name }

    @Test
    fun `a project is known once created, follows its renames, and stops being active when archived`() {
        publish(TaskKafkaTopics.Consumed.PROJECT_CREATED, projectCreated())
        awaitProjectNamed("Apollo")

        publish(
            TaskKafkaTopics.Consumed.PROJECT_UPDATED,
            ProjectUpdatedEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setName("Artemis")
                .setHiddenWorkLogEnabled(true)
                .build(),
        )
        awaitProjectNamed("Artemis")
        publish(
            TaskKafkaTopics.Consumed.PROJECT_ARCHIVED,
            ProjectArchivedEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setSagaId(null)
                .build(),
        )

        await().atMost(TIMEOUT).until { directory.findProject(projectId)?.isActive == false }
    }

    @Test
    fun `a redelivered event changes nothing`() {
        val eventId = Events.newId()
        publish(TaskKafkaTopics.Consumed.PROJECT_CREATED, projectCreated(), eventId)
        awaitProjectNamed("Apollo")

        publish(TaskKafkaTopics.Consumed.PROJECT_CREATED, projectCreated("Overwritten"), eventId)
        publish(
            TaskKafkaTopics.Consumed.PROJECT_MEMBER_JOINED,
            ProjectMemberJoinedEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setMemberId(Events.newId())
                .setUserId(userId.toString())
                .setRoleCode("MEMBER")
                .build(),
        )

        await().atMost(TIMEOUT).until { directory.findMembership(projectId, userId) != null }
        assertEquals("Apollo", directory.findProject(projectId)?.name)
    }

    @Test
    fun `members come and go`() {
        publish(TaskKafkaTopics.Consumed.PROJECT_CREATED, projectCreated())
        awaitProjectNamed("Apollo")
        publish(
            TaskKafkaTopics.Consumed.PROJECT_MEMBER_JOINED,
            ProjectMemberJoinedEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setMemberId(Events.newId())
                .setUserId(userId.toString())
                .setRoleCode("MEMBER")
                .build(),
        )
        await().atMost(TIMEOUT).until { directory.findMembership(projectId, userId)?.roleCode == "MEMBER" }

        publish(
            TaskKafkaTopics.Consumed.PROJECT_MEMBER_REMOVED,
            ProjectMemberRemovedEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setProjectId(projectId.toString())
                .setUserId(userId.toString())
                .build(),
        )

        await().atMost(TIMEOUT).until { directory.findMembership(projectId, userId) == null }
    }

    @Test
    fun `categories and statuses are kept until they are removed`() {
        publish(TaskKafkaTopics.Consumed.PROJECT_CREATED, projectCreated())
        awaitProjectNamed("Apollo")
        val categoryId = Uuid.generateV7().toJavaUuid().toString()
        val statusId = Uuid.generateV7().toJavaUuid().toString()

        publish(TaskKafkaTopics.Consumed.PROJECT_CATEGORY_CHANGED, category(categoryId, removed = false))
        publish(TaskKafkaTopics.Consumed.PROJECT_STATUS_CHANGED, status(statusId, removed = false))
        await().atMost(TIMEOUT).until {
            directory.findCategories(projectId).size == 1 && directory.findStatuses(projectId).size == 1
        }

        publish(TaskKafkaTopics.Consumed.PROJECT_CATEGORY_CHANGED, category(categoryId, removed = true))
        publish(TaskKafkaTopics.Consumed.PROJECT_STATUS_CHANGED, status(statusId, removed = true))
        await().atMost(TIMEOUT).until {
            directory.findCategories(projectId).isEmpty() && directory.findStatuses(projectId).isEmpty()
        }
    }

    private fun category(
        categoryId: String,
        removed: Boolean,
    ) = ProjectCategoryChangedEvent
        .newBuilder()
        .setEventId(Events.newId())
        .setTimestamp(Events.now())
        .setProjectId(projectId.toString())
        .setCategoryId(categoryId)
        .setNames(mapOf("en" to "Bug"))
        .setColor("#ff0000")
        .setRemoved(removed)
        .build()

    private fun status(
        statusId: String,
        removed: Boolean,
    ) = ProjectStatusChangedEvent
        .newBuilder()
        .setEventId(Events.newId())
        .setTimestamp(Events.now())
        .setProjectId(projectId.toString())
        .setStatusId(statusId)
        .setNames(mapOf("en" to "Doing"))
        .setColor("#00ff00")
        .setRemoved(removed)
        .build()

    @Test
    fun `a user is known once registered and follows their profile`() {
        publish(
            USER_REGISTERED,
            UserRegisteredEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setUserId(userId.toString())
                .setEmail("ada@example.com")
                .setFirstName("Ada")
                .setLastName("Byron")
                .build(),
        )
        await().atMost(TIMEOUT).until { users.findById(userId)?.lastName == "Byron" }

        publish(
            USER_PROFILE_UPDATED,
            UserProfileUpdatedEvent
                .newBuilder()
                .setEventId(Events.newId())
                .setTimestamp(Events.now())
                .setUserId(userId.toString())
                .setEmail("ada@example.com")
                .setFirstName("Ada")
                .setLastName("Lovelace")
                .setAvatarFileId(null)
                .build(),
        )

        await().atMost(TIMEOUT).until { users.findById(userId)?.lastName == "Lovelace" }
    }

    @Test
    fun `a role's permissions are projected and dropped with the role`() {
        val role = "ROLE_${userId.toString().take(8).uppercase()}"
        publish(TaskKafkaTopics.Consumed.ROLE_PERMISSIONS_CHANGED, roleChanged(role, removed = false))
        await().atMost(TIMEOUT).until { roles.findByName(role) != null }

        publish(TaskKafkaTopics.Consumed.ROLE_PERMISSIONS_CHANGED, roleChanged(role, removed = true))

        await().atMost(TIMEOUT).until { roles.findByName(role) == null }
    }

    private fun roleChanged(
        role: String,
        removed: Boolean,
    ) = RolePermissionsChangedEvent
        .newBuilder()
        .setEventId(Events.newId())
        .setTimestamp(Events.now())
        .setRole(role)
        .setScope("PROJECT")
        .setRemoved(removed)
        .setUnrestricted(false)
        .setPermissions(listOf("VIEW_TASKS"))
        .build()

    private companion object {
        const val USER_REGISTERED = "user-registered"
        const val USER_PROFILE_UPDATED = "user-profile-updated"
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
