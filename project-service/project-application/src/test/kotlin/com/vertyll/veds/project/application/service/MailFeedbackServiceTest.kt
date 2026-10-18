package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.RecordingSagaProcess
import com.vertyll.veds.project.application.SilentLogger
import com.vertyll.veds.project.application.saga.model.SagaTypes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MailFeedbackServiceTest {
    private val saga = RecordingSagaProcess()
    private val service = MailFeedbackService(saga, SilentLogger)

    private fun givenOwnSaga() = saga.startSaga(SagaTypes.PROJECT_INVITATION, emptyMap()).also { saga.trail.clear() }

    @Test
    fun `a delivered mail completes the saga that asked for it`() {
        val own = givenOwnSaga()

        service.handleMailSent(own.id, to = INVITEE_EMAIL)

        assertEquals(listOf("completed"), saga.trail)
    }

    @Test
    fun `a failed delivery fails the saga and keeps the reason`() {
        val own = givenOwnSaga()

        service.handleMailFailed(own.id, to = INVITEE_EMAIL, error = MAILBOX_FULL)

        assertEquals(listOf("failed(Mail delivery failed: mailbox full)"), saga.trail)
    }

    @Test
    fun `a saga belonging to another service is ignored`() {
        service.handleMailSent("someone-elses-saga", to = INVITEE_EMAIL)
        service.handleMailFailed("someone-elses-saga", to = INVITEE_EMAIL, error = MAILBOX_FULL)

        assertTrue(saga.trail.isEmpty())
    }

    @Test
    fun `a mail sent outside any saga is ignored`() {
        service.handleMailSent(null, to = INVITEE_EMAIL)
        service.handleMailFailed(null, to = INVITEE_EMAIL, error = MAILBOX_FULL)

        assertTrue(saga.trail.isEmpty())
    }
}

private const val INVITEE_EMAIL = "invitee@example.com"
private const val MAILBOX_FULL = "mailbox full"
