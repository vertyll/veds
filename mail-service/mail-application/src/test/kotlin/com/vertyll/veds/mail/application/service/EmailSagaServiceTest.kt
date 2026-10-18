package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.application.port.outbound.MailFeedbackEventPublisherPort
import com.vertyll.veds.mail.application.port.outbound.UseCaseLogger
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaSnapshot
import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.SagaTypeValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailSagaServiceTest {
    private val sagaTrail = mutableListOf<String>()
    private val feedback = mutableListOf<String>()

    private var sendSucceeds = true
    private var sendThrows: Exception? = null
    private var feedbackThrows: Exception? = null

    private val saga =
        object : SagaProcessPort {
            override fun startSaga(
                sagaType: SagaTypeValue,
                payload: Map<String, Any?>,
            ): SagaSnapshot {
                sagaTrail += "start(${sagaType.value})"
                return SagaSnapshot(id = "mail-saga-1", type = sagaType.value, status = SagaStatus.STARTED, payload = payload.toString())
            }

            override fun recordSagaStep(
                sagaId: String,
                stepName: SagaTypeValue,
                status: SagaStepStatus,
                payload: Map<String, Any?>,
            ) {
                sagaTrail += "step(${stepName.value},$status)"
            }

            override fun markSagaCompleted(sagaId: String) {
                sagaTrail += "completed"
            }

            override fun markSagaFailed(
                sagaId: String,
                errorMessage: String,
            ) {
                sagaTrail += "failed"
            }

            override fun markAwaitingResponse(sagaId: String) {
                sagaTrail += "awaiting"
            }

            override fun findSagaById(sagaId: String): SagaSnapshot? = null
        }

    private val emails =
        object : EmailUseCase {
            override fun sendEmail(
                to: String,
                template: EmailTemplate,
                variables: Map<String, String>,
                replyTo: String?,
            ): Boolean {
                sendThrows?.let { throw it }
                return sendSucceeds
            }

            override fun getEmailLogs() = error("not used by the saga")
        }

    private val publisher =
        object : MailFeedbackEventPublisherPort {
            override fun publishMailSent(
                originSagaId: String,
                to: String,
                subject: String,
                originalEventId: String,
            ) {
                feedbackThrows?.let { throw it }
                feedback += "sent($originSagaId,$originalEventId)"
            }

            override fun publishMailFailed(
                originSagaId: String,
                to: String,
                subject: String,
                originalEventId: String,
                error: String,
            ) {
                feedbackThrows?.let { throw it }
                feedback += "failed($originSagaId,$error)"
            }
        }

    private val logger =
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

    private val service = EmailSagaService(saga, emails, publisher, logger)

    private fun send(
        templateName: String = EmailTemplate.PROJECT_INVITATION.templateName,
        originSagaId: String? = "origin-saga",
        originalEventId: String? = "event-1",
    ) = service.sendEmailWithSaga(
        to = "ada@example.com",
        templateName = templateName,
        variables = mapOf("firstName" to "Ada"),
        replyTo = null,
        originSagaId = originSagaId,
        originalEventId = originalEventId,
    )

    @Test
    fun `a delivered mail completes its saga and tells the requester`() {
        assertTrue(send())

        assertEquals(
            listOf("start(EmailSending)", "step(ProcessTemplate,COMPLETED)", "step(SendEmail,COMPLETED)", "completed"),
            sagaTrail,
        )
        assertEquals(listOf("sent(origin-saga,event-1)"), feedback)
    }

    @Test
    fun `a refused delivery is reported back rather than swallowed`() {
        sendSucceeds = false

        assertFalse(send())

        assertTrue(sagaTrail.contains("step(SendEmail,FAILED)"))
        assertFalse(sagaTrail.contains("completed"))
        assertEquals(listOf("failed(origin-saga,Failed to send email)"), feedback)
    }

    @Test
    fun `an exception during delivery still produces an answer`() {
        sendThrows = IllegalStateException("SMTP server refused the connection")

        assertFalse(send())

        assertTrue(sagaTrail.contains("step(SendEmail,FAILED)"))
        assertEquals(listOf("failed(origin-saga,SMTP server refused the connection)"), feedback)
    }

    @Test
    fun `an unknown template is refused before any saga opens`() {
        assertFalse(send(templateName = "NO_SUCH_TEMPLATE"))

        assertTrue(sagaTrail.isEmpty())
        assertEquals(listOf("failed(origin-saga,Invalid template name: NO_SUCH_TEMPLATE)"), feedback)
    }

    @Test
    fun `a mail sent outside any saga publishes no feedback`() {
        assertTrue(send(originSagaId = null))

        assertTrue(feedback.isEmpty())
        assertTrue(sagaTrail.contains("completed"), "the local saga is still recorded")
    }

    @Test
    fun `an unknown template outside a saga reports nothing to nobody`() {
        assertFalse(send(templateName = "NO_SUCH_TEMPLATE", originSagaId = null))

        assertTrue(feedback.isEmpty())
        assertTrue(sagaTrail.isEmpty())
    }

    @Test
    fun `a delivered mail stays delivered even if the announcement fails`() {
        feedbackThrows = IllegalStateException("broker unavailable")

        assertTrue(send())

        assertTrue(sagaTrail.contains("completed"))
    }

    @Test
    fun `a request with no event id is identified by the mail saga`() {
        assertTrue(send(originalEventId = null))

        assertEquals(listOf("sent(origin-saga,mail-saga-1)"), feedback)
    }
}
