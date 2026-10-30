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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.assertEquals

@AutoConfigureMockMvc
internal class NotificationActionsIntegrationTest(
    private val mockMvc: MockMvc,
    private val notifications: NotificationRepository,
) : IntegrationTestBase() {
    private val reader = UUID.randomUUID()

    private fun MockHttpServletRequestBuilder.asReader() = with(jwt().jwt { it.subject(reader.toString()) })

    private fun MockHttpServletRequestBuilder.json(body: String) = contentType(MediaType.APPLICATION_JSON).content(body)

    private fun raised() = notifications.save(Notification.create(reader, NotificationType.TASK_ASSIGNED))

    @Test
    fun `a read notification leaves the unread count`() {
        val notification = raised()
        raised()

        mockMvc
            .perform(post("/notifications/mark-read").asReader().json("""{"notificationIds":["${notification.id}"]}"""))
            .andExpect(status().is2xxSuccessful)

        mockMvc
            .perform(get("/notifications/unread-count").asReader())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unread").value(1))
    }

    @Test
    fun `everything is marked read at once`() {
        raised()
        raised()

        mockMvc.perform(post("/notifications/mark-all-read").asReader()).andExpect(status().is2xxSuccessful)

        assertEquals(0, notifications.countUnread(reader))
    }

    @Test
    fun `a dismissed notification leaves the inbox`() {
        val notification = raised()

        mockMvc
            .perform(post("/notifications/dismiss").asReader().json("""{"notificationIds":["${notification.id}"]}"""))
            .andExpect(status().is2xxSuccessful)

        assertEquals(emptyList(), notifications.findAllActiveBy(reader))
    }

    @Test
    fun `the inbox is cleared at once`() {
        raised()
        raised()

        mockMvc.perform(post("/notifications/dismiss-all").asReader()).andExpect(status().is2xxSuccessful)

        assertEquals(emptyList(), notifications.findAllActiveBy(reader))
    }

    @Test
    fun `someone else's notification cannot be marked read`() {
        val notification = notifications.save(Notification.create(UUID.randomUUID(), NotificationType.TASK_ASSIGNED))

        mockMvc
            .perform(post("/notifications/mark-read").asReader().json("""{"notificationIds":["${notification.id}"]}"""))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `a muted type stays in the settings`() {
        mockMvc
            .perform(
                put("/notifications/settings")
                    .asReader()
                    .json("""{"mutedTypes":["TASK_CREATED"],"emailEnabledTypes":["TASK_ASSIGNED"]}"""),
            ).andExpect(status().isOk)

        mockMvc
            .perform(get("/notifications/settings").asReader())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mutedTypes[0]").value("TASK_CREATED"))
            .andExpect(jsonPath("$.emailEnabledTypes[0]").value("TASK_ASSIGNED"))
    }
}
