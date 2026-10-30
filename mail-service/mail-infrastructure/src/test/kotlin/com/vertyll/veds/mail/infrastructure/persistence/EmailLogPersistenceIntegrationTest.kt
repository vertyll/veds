package com.vertyll.veds.mail.infrastructure.persistence

import com.vertyll.veds.mail.domain.model.EmailLog
import com.vertyll.veds.mail.domain.model.EmailStatus
import com.vertyll.veds.mail.domain.repository.EmailLogRepository
import com.vertyll.veds.mail.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class EmailLogPersistenceIntegrationTest(
    private val logs: EmailLogRepository,
) : IntegrationTestBase() {
    private fun log(
        recipient: String,
        templateName: String = "TASK_CREATED",
    ) = EmailLog(recipient = recipient, subject = "New task", templateName = templateName)

    @Test
    fun `a log is stored and read back with every field`() {
        val saved = logs.save(log("stored@example.com").copy(variables = "a=b", replyTo = "owner@example.com"))

        val read = assertNotNull(logs.findById(assertNotNull(saved.id)))

        assertEquals("stored@example.com", read.recipient)
        assertEquals("a=b", read.variables)
        assertEquals("owner@example.com", read.replyTo)
        assertEquals(EmailStatus.PENDING, read.status)
    }

    @Test
    fun `an unknown id finds nothing`() {
        assertNull(logs.findById(Long.MAX_VALUE))
    }

    @Test
    fun `logs are found by their recipient and their template`() {
        logs.save(log("found@example.com", templateName = "TASK_COMMENT_ADDED"))
        logs.save(log("other@example.com", templateName = "TASK_ASSIGNED"))

        assertEquals(listOf("found@example.com"), logs.findByRecipient("found@example.com").map { it.recipient })
        assertTrue(logs.findByTemplateName("TASK_COMMENT_ADDED").all { it.templateName == "TASK_COMMENT_ADDED" })
        assertTrue(logs.findByTemplateName("TASK_COMMENT_ADDED").isNotEmpty())
    }

    @Test
    fun `sent logs are found and counted by when they went out`() {
        val before = Instant.now().minus(Duration.ofMinutes(1))
        val sent = logs.save(log("sent@example.com").markAsSent())
        val after = Instant.now().plus(Duration.ofMinutes(1))

        assertTrue(logs.findBySentAtBetween(before, after).any { it.id == sent.id })
        assertTrue(logs.countByStatusAndSentAtBetween(EmailStatus.SENT, before, after) >= 1)
        assertEquals(emptyList(), logs.findBySentAtBetween(after, after.plusSeconds(1)))
    }

    @Test
    fun `the most recent failures come first, as many as asked for`() {
        logs.save(log("first-failure@example.com").copy(status = EmailStatus.FAILED, createdAt = Instant.now().minusSeconds(60)))
        logs.save(log("last-failure@example.com").copy(status = EmailStatus.FAILED))

        val failures = logs.findRecentFailedEmails(1)

        assertEquals(listOf("last-failure@example.com"), failures.map { it.recipient })
    }
}
