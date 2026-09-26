package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.port.outbound.MailSenderPort
import com.vertyll.veds.mail.application.port.outbound.TemplateRendererPort
import com.vertyll.veds.mail.application.port.outbound.UseCaseLogger
import com.vertyll.veds.mail.domain.model.EmailLog
import com.vertyll.veds.mail.domain.model.EmailStatus
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.mail.domain.model.SenderAddress
import com.vertyll.veds.mail.domain.repository.EmailLogRepository
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmailServiceTest {
    private val saved = mutableListOf<EmailLog>()

    private val repository =
        object : EmailLogRepository {
            override fun save(emailLog: EmailLog): EmailLog = emailLog.also { saved += it }

            override fun findById(id: Long): EmailLog? = null

            override fun findByRecipient(recipient: String): List<EmailLog> = emptyList()

            override fun findByTemplateName(templateName: String): List<EmailLog> = emptyList()

            override fun findBySentAtBetween(
                start: Instant,
                end: Instant,
            ): List<EmailLog> = emptyList()

            override fun countByStatusAndSentAtBetween(
                status: EmailStatus,
                start: Instant,
                end: Instant,
            ): Long = 0

            override fun findRecentFailedEmails(limit: Int): List<EmailLog> = emptyList()
        }

    private val silentLogger =
        object : UseCaseLogger {
            override fun debug(
                message: String,
                vararg args: Any?,
            ) = Unit

            override fun info(
                message: String,
                vararg args: Any?,
            ) = Unit

            override fun warn(
                message: String,
                vararg args: Any?,
            ) = Unit

            override fun error(
                message: String,
                vararg args: Any?,
            ) = Unit
        }

    private fun renderer(output: String = "<p>hello</p>") =
        object : TemplateRendererPort {
            override fun render(
                templateName: String,
                variables: Map<String, String>,
            ): String = output
        }

    private fun sender(failWith: Exception? = null) =
        object : MailSenderPort {
            override fun sendHtml(
                from: String,
                to: String,
                subject: String,
                htmlContent: String,
                replyTo: String?,
            ) {
                failWith?.let { throw it }
            }
        }

    private fun service(
        mailSender: MailSenderPort = sender(),
        templateRenderer: TemplateRendererPort = renderer(),
    ) = EmailService(mailSender, templateRenderer, repository, SenderAddress("no-reply@veds.local"), silentLogger)

    private val template = EmailTemplate.entries.first()

    @Test
    fun `records a sent message with the moment it left`() {
        val sent = service().sendEmail(A_EXAMPLE_COM, template, emptyMap(), null)

        assertTrue(sent)
        assertEquals(1, saved.size)
        assertEquals(EmailStatus.SENT, saved.single().status)
        assertNotNull(saved.single().sentAt)
    }

    @Test
    fun `reports a failure to the caller instead of throwing`() {
        val sent =
            service(mailSender = sender(failWith = IllegalStateException(SMTP_REFUSED))).sendEmail(
                A_EXAMPLE_COM,
                template,
                emptyMap(),
                null,
            )

        assertFalse(sent, "a delivery failure is an outcome the caller has to see, not an exception it must catch")
    }

    @Test
    fun `records a failed attempt with its reason and no send time`() {
        service(mailSender = sender(failWith = IllegalStateException(SMTP_REFUSED))).sendEmail(
            A_EXAMPLE_COM,
            template,
            emptyMap(),
            null,
        )

        val log = saved.single()
        assertEquals(EmailStatus.FAILED, log.status)
        assertEquals(SMTP_REFUSED, log.errorMessage)
        assertNull(log.sentAt, "a message that never left has no send time")
    }

    @Test
    fun `truncates a long variable so the log entry still fits its column`() {
        service().sendEmail(A_EXAMPLE_COM, template, mapOf("body" to "x".repeat(200)), null)

        val stored = saved.single().variables
        assertNotNull(stored)
        assertTrue(stored.endsWith("..."), "a truncated value is marked as truncated")
        assertTrue(stored.length < 200, "the stored value is shorter than the input")
    }

    @Test
    fun `stores no variables when there are none`() {
        service().sendEmail(A_EXAMPLE_COM, template, emptyMap(), null)

        assertNull(saved.single().variables)
    }
}

private const val A_EXAMPLE_COM = "a@example.com"
private const val SMTP_REFUSED = "SMTP refused"
