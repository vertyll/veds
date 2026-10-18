@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.application.command.CreateProjectCommand
import com.vertyll.veds.project.application.command.InviteMemberCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.port.inbound.MailFeedbackUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectCommandUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectInvitationCommandUseCase
import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import com.vertyll.veds.project.infrastructure.IntegrationTestBase
import com.vertyll.veds.project.infrastructure.kafka.ProjectKafkaTopics
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectInvitationSagaIntegrationTest(
    private val projectCommands: ProjectCommandUseCase,
    private val invitationCommands: ProjectInvitationCommandUseCase,
    private val invitationRepository: ProjectInvitationRepository,
    private val outboxRepository: OutboxRepository,
    private val mailFeedback: MailFeedbackUseCase,
    private val sagaProcess: SagaProcessPort,
) : IntegrationTestBase() {
    @BeforeEach
    fun clearOutbox() {
        outboxRepository.deleteAll()
    }

    private fun owner() =
        Actor(
            id = Uuid.generateV7().toJavaUuid(),
            email = "owner@example.com",
            firstName = "Ada",
            lastName = "Lovelace",
        )

    private fun projectFor(actor: Actor) =
        projectCommands.createProject(
            CreateProjectCommand(
                name = "Apollo",
                description = null,
                isPublic = false,
                typeId = null,
                iconFileId = null,
            ),
            actor,
        )

    @Test
    fun `inviting writes the invitation and its event in one transaction`() {
        val actor = owner()
        val project = projectFor(actor)

        val invitation =
            invitationCommands.invite(
                project.id,
                InviteMemberCommand(email = "invitee@example.com", roleId = null),
                actor.id,
            )

        val stored = invitationRepository.findById(invitation.id)
        assertNotNull(stored)
        assertEquals(InvitationStatus.PENDING, stored.status)

        val queued = outboxRepository.findAll().filter { it.topic == ProjectKafkaTopics.PROJECT_MEMBER_INVITED }
        assertTrue(queued.isNotEmpty(), "the invitation event must be in the outbox, not sent directly to Kafka")
    }

    @Test
    fun `a failed delivery expires the invitation through compensation`() {
        val actor = owner()
        val project = projectFor(actor)

        invitationCommands.invite(
            project.id,
            InviteMemberCommand(email = "bounces@example.com", roleId = null),
            actor.id,
        )

        val sagaId = openSagaId()

        mailFeedback.handleMailFailed(
            sagaId = sagaId,
            to = "bounces@example.com",
            error = "550 mailbox unavailable",
        )

        val saga = sagaProcess.findSagaById(sagaId)
        assertNotNull(saga)
        assertTrue(
            saga.status in setOf(SagaStatus.COMPENSATING, SagaStatus.COMPENSATED, SagaStatus.FAILED),
            "a bounced invitation must not leave the saga open, was ${saga.status}",
        )
    }

    @Test
    fun `a delivered invitation closes the saga so the watchdog cannot revoke it`() {
        val actor = owner()
        val project = projectFor(actor)

        val invitation =
            invitationCommands.invite(
                project.id,
                InviteMemberCommand(email = "delivered@example.com", roleId = null),
                actor.id,
            )
        val sagaId = openSagaId()

        mailFeedback.handleMailSent(sagaId, "delivered@example.com")

        val saga = sagaProcess.findSagaById(sagaId)
        assertNotNull(saga)
        assertEquals(
            SagaStatus.COMPLETED,
            saga.status,
            "a saga left open after delivery is timed out by the watchdog, which revokes the invitation",
        )

        val stored = invitationRepository.findById(invitation.id)
        assertNotNull(stored)
        assertEquals(InvitationStatus.PENDING, stored.status)
    }

    @Test
    fun `replaying the same mail feedback is a no-op`() {
        val actor = owner()
        val project = projectFor(actor)

        val invitation =
            invitationCommands.invite(
                project.id,
                InviteMemberCommand(email = DUPLICATE_EXAMPLE_COM, roleId = null),
                actor.id,
            )
        val sagaId = openSagaId()

        mailFeedback.handleMailSent(sagaId, DUPLICATE_EXAMPLE_COM)
        val afterFirst = sagaProcess.findSagaById(sagaId)?.status

        mailFeedback.handleMailSent(sagaId, DUPLICATE_EXAMPLE_COM)
        val afterSecond = sagaProcess.findSagaById(sagaId)?.status

        assertEquals(afterFirst, afterSecond, "at-least-once delivery must not move a settled saga")

        val stored = invitationRepository.findById(invitation.id)
        assertNotNull(stored)
        assertEquals(InvitationStatus.PENDING, stored.status)
    }

    private fun openSagaId(): String {
        val message =
            outboxRepository
                .findAll()
                .firstOrNull { it.topic == ProjectKafkaTopics.PROJECT_MEMBER_INVITED && it.sagaId != null }
        assertNotNull(message, "no outbox message carries a sagaId")
        return message.sagaId!!
    }
}

private const val DUPLICATE_EXAMPLE_COM = "duplicate@example.com"
