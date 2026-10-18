package com.vertyll.veds.mail.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EmailLogTest {
    private fun log() =
        EmailLog(
            recipient = "ada@example.com",
            subject = "Activate your account",
            templateName = EmailTemplate.PROJECT_INVITATION.templateName,
        )

    @Test
    fun `a new entry is pending and has not been sent`() {
        val entry = log()

        assertEquals(EmailStatus.PENDING, entry.status)
        assertNull(entry.sentAt)
        assertNull(entry.errorMessage)
    }

    @Test
    fun `a sent mail records when it went out`() {
        val entry = log().markAsSent()

        assertEquals(EmailStatus.SENT, entry.status)
        assertNotNull(entry.sentAt)
    }

    @Test
    fun `a failed mail keeps the reason it failed`() {
        val entry = log().markAsFailed(MAILBOX_DOES_NOT_EXIST)

        assertEquals(EmailStatus.FAILED, entry.status)
        assertEquals(MAILBOX_DOES_NOT_EXIST, entry.errorMessage)
    }

    @Test
    fun `a failed mail carries no send time`() {
        assertNull(log().markAsFailed(MAILBOX_DOES_NOT_EXIST).sentAt)
    }

    @Test
    fun `recording an outcome does not disturb what was sent`() {
        val entry = log()

        listOf(entry.markAsSent(), entry.markAsFailed("refused")).forEach { after ->
            assertEquals(entry.recipient, after.recipient)
            assertEquals(entry.subject, after.subject)
            assertEquals(entry.templateName, after.templateName)
            assertEquals(entry.createdAt, after.createdAt)
        }
    }
}

private const val MAILBOX_DOES_NOT_EXIST = "mailbox does not exist"
