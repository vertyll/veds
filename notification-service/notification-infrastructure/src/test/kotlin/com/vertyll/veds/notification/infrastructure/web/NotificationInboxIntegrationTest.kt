package com.vertyll.veds.notification.infrastructure.web

import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import com.vertyll.veds.notification.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@AutoConfigureMockMvc
internal class NotificationInboxIntegrationTest(
    private val mockMvc: MockMvc,
    private val notifications: NotificationRepository,
) : IntegrationTestBase() {
    private fun caller(id: UUID) = jwt().jwt { it.subject(id.toString()) }

    @Test
    fun `the health endpoint is open and the inbox needs a token`() {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk)

        mockMvc.perform(get("/notifications")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `the inbox lists only the caller's own notifications`() {
        val reader = UUID.randomUUID()
        val own = notifications.save(Notification.create(reader, NotificationType.TASK_ASSIGNED))
        notifications.save(Notification.create(UUID.randomUUID(), NotificationType.TASK_ASSIGNED))

        mockMvc
            .perform(get("/notifications").with(caller(reader)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pagination.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value(own.id.toString()))
    }

    @Test
    fun `the inbox narrows to the project and the type asked for`() {
        val reader = UUID.randomUUID()
        val project = UUID.randomUUID()
        val wanted = notifications.save(Notification.create(reader, NotificationType.TASK_CREATED, projectId = project))
        notifications.save(Notification.create(reader, NotificationType.TASK_ASSIGNED, projectId = project))
        notifications.save(Notification.create(reader, NotificationType.TASK_CREATED))

        mockMvc
            .perform(
                get("/notifications")
                    .param("projectId", project.toString())
                    .param("type", NotificationType.TASK_CREATED.name)
                    .with(caller(reader)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.pagination.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value(wanted.id.toString()))
    }

    @Test
    fun `the inbox leaves out what was read when only unread ones are asked for`() {
        val reader = UUID.randomUUID()
        val unread = notifications.save(Notification.create(reader, NotificationType.TASK_CREATED))
        notifications.save(Notification.create(reader, NotificationType.TASK_ASSIGNED).markRead())

        mockMvc
            .perform(get("/notifications").param("onlyUnread", "true").with(caller(reader)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pagination.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value(unread.id.toString()))
    }

    @Test
    fun `a token whose subject is no identifier is refused, naming the claim`() {
        mockMvc
            .perform(get("/notifications").with(jwt().jwt { it.subject("not-a-uuid") }))
            .andExpect(status().isForbidden)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
    }
}
