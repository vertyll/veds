package com.vertyll.veds.mail.infrastructure.web

import com.vertyll.veds.mail.domain.model.EmailLog
import com.vertyll.veds.mail.domain.model.EmailStatus
import com.vertyll.veds.mail.domain.repository.EmailLogRepository
import com.vertyll.veds.mail.infrastructure.IntegrationTestBase
import com.vertyll.veds.mail.infrastructure.RecordingMailSender
import com.vertyll.veds.mail.infrastructure.persistence.entity.RolePermissionsJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.repository.RolePermissionsJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

@AutoConfigureMockMvc
internal class EmailApiIntegrationTest(
    private val mockMvc: MockMvc,
    private val roles: RolePermissionsJpaRepository,
    private val mailServer: RecordingMailSender,
    private val logs: EmailLogRepository,
) : IntegrationTestBase() {
    private fun postman() = jwt().authorities(SimpleGrantedAuthority("ROLE_$ROLE"))

    private fun log(
        recipient: String,
        createdAt: Instant,
    ) = EmailLog(recipient = recipient, subject = "New task", templateName = "TASK_CREATED", createdAt = createdAt)

    private fun send(
        to: String,
        templateName: String = "PROJECT_MEMBER_JOINED",
    ) = """{"to":"$to","templateName":"$templateName","variables":{"projectName":"Apollo"}}"""

    @BeforeEach
    fun grantTheRole() {
        roles.save(RolePermissionsJpaEntity(roleName = ROLE, unrestricted = true))
    }

    @Test
    fun `the health endpoint is open and the logs need a token`() {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk)

        mockMvc.perform(get("/mail/logs")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `a caller without the permission sends nothing`() {
        mockMvc
            .perform(
                post("/mail/send")
                    .with(jwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(send("stranger@example.com")),
            ).andExpect(status().isForbidden)

        assertEquals(emptyList(), mailServer.deliveredTo("stranger@example.com"))
    }

    @Test
    fun `an email goes out and is logged as sent`() {
        mockMvc
            .perform(
                post("/mail/send")
                    .with(postman())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(send("reader@example.com")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        assertEquals(1, mailServer.deliveredTo("reader@example.com").size)
        assertEquals(listOf(EmailStatus.SENT), logs.findByRecipient("reader@example.com").map { it.status })
    }

    @Test
    fun `the logs endpoint pages through what was logged, newest first`() {
        val later = Instant.now().plus(1, ChronoUnit.DAYS)
        logs.save(log(recipient = "older@example.com", createdAt = later))
        logs.save(log(recipient = "newer@example.com", createdAt = later.plusSeconds(60)))

        mockMvc
            .perform(get("/mail/logs").param("page", "0").param("size", "1").with(postman()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].recipient").value("newer@example.com"))
        mockMvc
            .perform(get("/mail/logs").param("page", "1").param("size", "1").with(postman()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].recipient").value("older@example.com"))
    }

    @Test
    fun `a refused delivery answers that it failed`() {
        mailServer.refused += "bounces@example.com"

        mockMvc
            .perform(
                post("/mail/send")
                    .with(postman())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(send("bounces@example.com")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `an unknown template is refused before anything is sent`() {
        mockMvc
            .perform(
                post("/mail/send")
                    .with(postman())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(send("unknown@example.com", templateName = "NO_SUCH_TEMPLATE")),
            ).andExpect(status().is4xxClientError)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))

        assertEquals(emptyList(), mailServer.deliveredTo("unknown@example.com"))
    }

    @Test
    fun `an address that is no address is refused`() {
        mockMvc
            .perform(
                post("/mail/send")
                    .with(postman())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(send("not-an-address")),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `a batch reports each recipient on its own`() {
        mailServer.refused += "batch-bounces@example.com"

        mockMvc
            .perform(
                post("/mail/send-batch")
                    .with(postman())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"recipients":["batch-reader@example.com","batch-bounces@example.com"],
                         "templateName":"PROJECT_MEMBER_JOINED",
                         "commonVariables":{"projectName":"Apollo"}}
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalRecipients").value(2))
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.failureCount").value(1))
    }

    @Test
    fun `a batch with an unknown template is refused`() {
        mockMvc
            .perform(
                post("/mail/send-batch")
                    .with(postman())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"recipients":["someone@example.com"],"templateName":"NO_SUCH_TEMPLATE"}"""),
            ).andExpect(status().is4xxClientError)
    }

    private companion object {
        const val ROLE = "POSTMAN"
    }
}
