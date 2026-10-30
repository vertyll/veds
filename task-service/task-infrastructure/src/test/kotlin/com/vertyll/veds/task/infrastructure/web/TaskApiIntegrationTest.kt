@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.infrastructure.web

import com.jayway.jsonpath.JsonPath
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import com.vertyll.veds.task.domain.model.ProjectCategoryRef
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.ProjectStatusRef
import com.vertyll.veds.task.domain.model.RolePermissionsRef
import com.vertyll.veds.task.domain.model.UserRef
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.domain.repository.RolePermissionsRepository
import com.vertyll.veds.task.domain.repository.UserDirectoryRepository
import com.vertyll.veds.task.infrastructure.IntegrationTestBase
import com.vertyll.veds.task.infrastructure.kafka.TaskKafkaTopics
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@AutoConfigureMockMvc
internal class TaskApiIntegrationTest(
    private val mockMvc: MockMvc,
    private val directory: ProjectDirectoryRepository,
    private val roles: RolePermissionsRepository,
    private val users: UserDirectoryRepository,
    private val outbox: OutboxRepository,
) : IntegrationTestBase() {
    private val projectId: UUID = Uuid.generateV7().toJavaUuid()
    private val ownerId: UUID = Uuid.generateV7().toJavaUuid()
    private val statusId: UUID = Uuid.generateV7().toJavaUuid()
    private val categoryId: UUID = Uuid.generateV7().toJavaUuid()

    private fun owner() = jwt().jwt { it.subject(ownerId.toString()).claim("email", "owner@example.com") }

    private fun stranger() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("email", "x@example.com") }

    private fun MockHttpServletRequestBuilder.json(body: String) = contentType(MediaType.APPLICATION_JSON).content(body)

    private fun MvcResult.id(): String = JsonPath.read(response.contentAsString, "$.id")

    private fun createTask(name: String = "Ship it"): MvcResult =
        mockMvc
            .perform(
                post("/tasks/project/{projectId}", projectId)
                    .with(owner())
                    .json(
                        """
                        {"name":"$name","description":"With care","priority":"HIGH","statusId":"$statusId",
                         "categoryIds":["$categoryId"],"assigneeIds":["$ownerId"],"priceEstimation":3}
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andReturn()

    @BeforeEach
    fun seedTheProject() {
        directory.saveProject(ProjectRef(projectId = projectId, name = "Apollo", hiddenWorkLogEnabled = true))
        roles.save(RolePermissionsRef(roleName = ROLE, unrestricted = true))
        directory.saveMembership(ProjectMembershipRef(projectId, ownerId, ROLE))
        directory.saveStatus(ProjectStatusRef(statusId, projectId, mapOf("en" to "Doing"), "#00ff00"))
        directory.saveCategory(ProjectCategoryRef(categoryId, projectId, mapOf("en" to "Bug"), "#ff0000"))
        users.save(UserRef(userId = ownerId, email = "owner@example.com", firstName = "Ada", lastName = "Lovelace"))
    }

    @Test
    fun `a created task is read back with an entity tag and announced`() {
        val taskId = createTask().id()

        mockMvc
            .perform(get("/tasks/{id}", taskId).with(owner()).header(LANGUAGE_HEADER, "en"))
            .andExpect(status().isOk)
            .andExpect(header().exists(HttpHeaders.ETAG))
            .andExpect(jsonPath("$.task.name").value("Ship it"))

        val topics = outbox.findAll().map { it.topic }
        assertTrue(TaskKafkaTopics.TASK_CREATED in topics)
        assertTrue(TaskKafkaTopics.TASK_ASSIGNED in topics)
    }

    @Test
    fun `tasks are listed and filtered within their project`() {
        createTask("Findable")

        mockMvc
            .perform(
                get("/tasks/project/{projectId}", projectId).with(owner()).header(LANGUAGE_HEADER, "en").param("searchTerm", "Findable"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].name").value("Findable"))
        mockMvc
            .perform(
                get(
                    "/tasks/project/{projectId}",
                    projectId,
                ).with(owner()).header(LANGUAGE_HEADER, "en").param("searchTerm", "Nothing like it"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)
    }

    @Test
    fun `a task is updated under its entity tag, and a stale tag is refused`() {
        val taskId = createTask().id()
        val tag =
            mockMvc
                .perform(
                    get("/tasks/{id}", taskId).with(owner()).header(LANGUAGE_HEADER, "en"),
                ).andReturn()
                .response
                .getHeader(HttpHeaders.ETAG)

        mockMvc
            .perform(
                put("/tasks/{id}", taskId)
                    .with(owner())
                    .header(HttpHeaders.IF_MATCH, tag.orEmpty())
                    .json("""{"name":"Renamed","priority":"LOW","statusId":"$statusId"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Renamed"))
        mockMvc
            .perform(
                put("/tasks/{id}", taskId)
                    .with(owner())
                    .header(HttpHeaders.IF_MATCH, tag.orEmpty())
                    .json("""{"name":"Too late"}"""),
            ).andExpect(status().isPreconditionFailed)
    }

    @Test
    fun `a task moves to another status and the move is announced`() {
        val otherStatus = Uuid.generateV7().toJavaUuid()
        directory.saveStatus(ProjectStatusRef(otherStatus, projectId, mapOf("en" to "Done"), "#0000ff"))
        val taskId = createTask().id()

        mockMvc
            .perform(patch("/tasks/{id}/status", taskId).with(owner()).json("""{"statusId":"$otherStatus"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusId").value(otherStatus.toString()))

        assertTrue(TaskKafkaTopics.TASK_STATUS_CHANGED in outbox.findAll().map { it.topic })
    }

    @Test
    fun `an archived task leaves the active list`() {
        val taskId = createTask().id()

        mockMvc.perform(delete("/tasks/{id}", taskId).with(owner())).andExpect(status().isOk)

        mockMvc
            .perform(get("/tasks/project/{projectId}", projectId).with(owner()).header(LANGUAGE_HEADER, "en"))
            .andExpect(jsonPath("$.items").isEmpty)
        assertTrue(TaskKafkaTopics.TASK_ARCHIVED in outbox.findAll().map { it.topic })
    }

    @Test
    fun `several tasks are archived at once and the count comes back`() {
        val first = createTask("First").id()
        val second = createTask("Second").id()

        mockMvc
            .perform(post("/tasks/batch-delete").with(owner()).json("""{"taskIds":["$first","$second"]}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(2))
    }

    @Test
    fun `a comment is added, edited, listed and removed`() {
        val taskId = createTask().id()

        val comment =
            mockMvc
                .perform(post("/tasks/{id}/comments", taskId).with(owner()).json("""{"content":"Looks good"}"""))
                .andExpect(status().is2xxSuccessful)
                .andReturn()
                .id()
        mockMvc
            .perform(put("/tasks/comments/{id}", comment).with(owner()).json("""{"content":"Looks great"}"""))
            .andExpect(status().isOk)
        mockMvc
            .perform(get("/tasks/{id}/comments", taskId).with(owner()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].content").value("Looks great"))
        mockMvc.perform(delete("/tasks/comments/{id}", comment).with(owner())).andExpect(status().is2xxSuccessful)

        mockMvc.perform(get("/tasks/{id}/comments", taskId).with(owner())).andExpect(jsonPath("$").isEmpty)
        assertTrue(TaskKafkaTopics.TASK_COMMENT_ADDED in outbox.findAll().map { it.topic })
    }

    @Test
    fun `work is logged, corrected, listed and removed`() {
        val taskId = createTask().id()

        val entry =
            mockMvc
                .perform(
                    post("/tasks/{id}/worklog", taskId)
                        .with(owner())
                        .json("""{"minutes":30,"workedOn":"2026-01-01","description":"Morning"}"""),
                ).andExpect(status().is2xxSuccessful)
                .andReturn()
                .id()
        mockMvc
            .perform(
                put("/tasks/worklog/{id}", entry)
                    .with(owner())
                    .json("""{"minutes":45,"workedOn":"2026-01-01","hidden":true}"""),
            ).andExpect(status().isOk)
        mockMvc
            .perform(get("/tasks/{id}/worklog", taskId).with(owner()))
            .andExpect(status().isOk)
        mockMvc.perform(delete("/tasks/worklog/{id}", entry).with(owner())).andExpect(status().is2xxSuccessful)
    }

    @Test
    fun `a member sees what the role allows`() {
        mockMvc
            .perform(get("/tasks/project/{projectId}/permissions", projectId).with(owner()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isNotEmpty)
    }

    @Test
    fun `someone outside the project creates nothing`() {
        mockMvc
            .perform(post("/tasks/project/{projectId}", projectId).with(stranger()).json("""{"name":"Intruder"}"""))
            .andExpect(status().isForbidden)
    }

    private companion object {
        const val ROLE = "PROJECT_OWNER"
        const val LANGUAGE_HEADER = "X-Lang"
    }
}
