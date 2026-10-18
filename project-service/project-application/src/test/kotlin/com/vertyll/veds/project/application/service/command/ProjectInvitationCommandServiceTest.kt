@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.InMemoryInvitationRepository
import com.vertyll.veds.project.application.InMemoryMemberRepository
import com.vertyll.veds.project.application.InMemoryProjectRepository
import com.vertyll.veds.project.application.InMemoryRoleRepository
import com.vertyll.veds.project.application.InMemoryUserDirectory
import com.vertyll.veds.project.application.RecordingEventPublisher
import com.vertyll.veds.project.application.RecordingSagaProcess
import com.vertyll.veds.project.application.SilentLogger
import com.vertyll.veds.project.application.actor
import com.vertyll.veds.project.application.command.InviteMemberCommand
import com.vertyll.veds.project.application.project
import com.vertyll.veds.project.application.role
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.model.ProjectInvitation
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectInvitationCommandServiceTest {
    private val invitations = InMemoryInvitationRepository()
    private val projects = InMemoryProjectRepository()
    private val members = InMemoryMemberRepository()
    private val roles = InMemoryRoleRepository()
    private val users = InMemoryUserDirectory()
    private val events = RecordingEventPublisher()
    private val saga = RecordingSagaProcess()

    private val memberRole = role(ProjectRoleCode.MEMBER).also { roles.given(it) }
    private val managerRole = role(ProjectRoleCode.MANAGER).also { roles.given(it) }

    private val service =
        ProjectInvitationCommandService(
            invitationRepository = invitations,
            memberRepository = members,
            roleRepository = roles,
            projectRepository = projects,
            userDirectory = users,
            authorization = ProjectAuthorizationService(projects, members, roles),
            eventPublisher = events,
            sagaProcess = saga,
            logger = SilentLogger,
        )

    private val owner = Uuid.generateV7().toJavaUuid()
    private val existing = project(ownerId = owner).also { projects.given(it) }

    private fun invite(
        email: String = NEW_EXAMPLE_COM,
        roleId: UUID? = null,
    ) = service.invite(existing.id, InviteMemberCommand(email = email, roleId = roleId), owner)

    @Test
    fun `an invitation is stored as pending against the project`() {
        val response = invite()

        val stored = invitations.findById(response.id)
        assertNotNull(stored)
        assertEquals(InvitationStatus.PENDING, stored.status)
        assertEquals(existing.id, stored.projectId)
        assertEquals(owner, stored.inviterId)
    }

    @Test
    fun `inviting asks for the mail through an event`() {
        val response = invite(email = NEW_EXAMPLE_COM)

        assertEquals(listOf("MemberInvited(${existing.id},new@example.com)"), events.published)
        assertEquals(
            response.id,
            invitations.stored.values
                .single()
                .id,
        )
    }

    @Test
    fun `the invitation saga is left awaiting the mail service`() {
        invite()

        assertEquals(
            listOf(
                "start(ProjectInvitation)",
                "step(PersistInvitation,COMPLETED)",
                "step(RequestInvitationMail,COMPLETED)",
                "awaiting",
            ),
            saga.trail,
        )
    }

    @Test
    fun `an explicit role is used when one is given`() {
        val response = invite(roleId = managerRole.id)

        assertEquals(managerRole.id, invitations.findById(response.id)!!.roleId)
    }

    @Test
    fun `without a role the invitee becomes a plain member`() {
        val response = invite()

        assertEquals(memberRole.id, invitations.findById(response.id)!!.roleId)
    }

    @Test
    fun `an unknown role is rejected`() {
        val error = assertFailsWith<ApiException> { invite(roleId = Uuid.generateV7().toJavaUuid()) }

        assertEquals(ProjectError.ROLE_NOT_FOUND, error.error)
    }

    @Test
    fun `a second invitation to a pending address is refused`() {
        invite(email = NEW_EXAMPLE_COM)

        val error = assertFailsWith<ApiException> { invite(email = NEW_EXAMPLE_COM) }

        assertEquals(ProjectError.INVITATION_ALREADY_SENT, error.error)
        assertEquals(1, invitations.stored.size)
    }

    @Test
    fun `an address whose invitation was rejected can be invited again`() {
        val first = invite(email = NEW_EXAMPLE_COM)
        invitations.save(invitations.findById(first.id)!!.reject(Uuid.generateV7().toJavaUuid()))

        val second = invite(email = NEW_EXAMPLE_COM)

        assertTrue(second.id != first.id)
    }

    @Test
    fun `someone without the invite permission cannot invite`() {
        val outsider = Uuid.generateV7().toJavaUuid()

        assertFailsWith<ApiException> { service.invite(existing.id, InviteMemberCommand(NEW_EXAMPLE_COM, null), outsider) }

        assertTrue(invitations.stored.isEmpty())
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `a request rejected up front never opens a saga`() {
        roles.stored.clear()

        assertFailsWith<ApiException> { invite() }

        assertTrue(saga.trail.isEmpty())
    }

    @Test
    fun `a failure after the saga has opened marks it failed`() {
        val failing =
            ProjectInvitationCommandService(
                invitationRepository = invitations,
                memberRepository = members,
                roleRepository = roles,
                projectRepository = projects,
                userDirectory = users,
                authorization = ProjectAuthorizationService(projects, members, roles),
                eventPublisher =
                    object : RecordingEventPublisher() {
                        override fun publishMemberInvited(
                            projectId: UUID,
                            projectName: String,
                            invitationId: UUID,
                            inviteeEmail: String,
                            inviterId: UUID,
                            sagaId: String?,
                        ): Unit = throw ApiException(ProjectError.INVITATION_ALREADY_SENT)
                    },
                sagaProcess = saga,
                logger = SilentLogger,
            )

        assertFailsWith<ApiException> {
            failing.invite(existing.id, InviteMemberCommand(NEW_EXAMPLE_COM, null), owner)
        }

        assertEquals("step(PersistInvitation,FAILED)", saga.trail[saga.trail.size - 2])
        assertTrue(saga.trail.last().startsWith("failed("))
    }

    private fun givenPendingInvitation(
        email: String = NEW_EXAMPLE_COM,
        roleId: UUID = memberRole.id,
        expiresAt: Instant = Instant.now().plus(7, ChronoUnit.DAYS),
    ) = ProjectInvitation(
        projectId = existing.id,
        inviteeEmail = email,
        inviterId = owner,
        roleId = roleId,
        expiresAt = expiresAt,
    ).also { invitations.given(it) }

    @Test
    fun `accepting turns the invitee into a member`() {
        val invitation = givenPendingInvitation()
        val invitee = actor(email = invitation.inviteeEmail)

        service.acceptInvitation(invitation.id, invitee)

        val membership = members.findByProjectIdAndUserId(existing.id, invitee.id)
        assertNotNull(membership)
        assertEquals(memberRole.id, membership.roleId)
    }

    @Test
    fun `accepting records who accepted and closes the invitation`() {
        val invitation = givenPendingInvitation()
        val invitee = actor(email = invitation.inviteeEmail)

        val response = service.acceptInvitation(invitation.id, invitee)

        val stored = invitations.findById(invitation.id)!!
        assertEquals(InvitationStatus.ACCEPTED, stored.status)
        assertEquals(invitee.id, stored.inviteeId)
        assertEquals(existing.name, response.projectName)
    }

    @Test
    fun `accepting announces the new member and registers them in the directory`() {
        val invitation = givenPendingInvitation()
        val invitee = actor(email = invitation.inviteeEmail)

        service.acceptInvitation(invitation.id, invitee)

        val membership = members.findByProjectIdAndUserId(existing.id, invitee.id)!!
        assertEquals(listOf("MemberJoined(${existing.id},${invitee.id},MEMBER)"), events.published)
        assertEquals(membership.userId, users.findById(invitee.id)!!.userId)
    }

    @Test
    fun `an invitation addressed to someone else cannot be accepted`() {
        val invitation = givenPendingInvitation(email = "intended@example.com")

        val error =
            assertFailsWith<ApiException> { service.acceptInvitation(invitation.id, actor(email = "someone.else@example.com")) }

        assertEquals(ProjectError.INVITATION_NOT_ADDRESSED_TO_CALLER, error.error)
        assertTrue(members.stored.isEmpty())
    }

    @Test
    fun `the invitee's address is matched regardless of case`() {
        val invitation = givenPendingInvitation(email = "Invitee@Example.com")

        service.acceptInvitation(invitation.id, actor(email = "invitee@example.com"))

        assertEquals(InvitationStatus.ACCEPTED, invitations.findById(invitation.id)!!.status)
    }

    @Test
    fun `an expired invitation is refused and recorded as expired`() {
        val invitation = givenPendingInvitation(expiresAt = Instant.now().minus(1, ChronoUnit.DAYS))

        val error =
            assertFailsWith<ApiException> { service.acceptInvitation(invitation.id, actor(email = invitation.inviteeEmail)) }

        assertEquals(ProjectError.INVITATION_EXPIRED, error.error)
        assertEquals(InvitationStatus.EXPIRED, invitations.findById(invitation.id)!!.status)
    }

    @Test
    fun `an invitation that was already settled cannot be accepted again`() {
        val invitation = givenPendingInvitation()
        invitations.save(invitation.accept(Uuid.generateV7().toJavaUuid()))

        val error =
            assertFailsWith<ApiException> { service.acceptInvitation(invitation.id, actor(email = invitation.inviteeEmail)) }

        assertEquals(ProjectError.INVITATION_NOT_PENDING, error.error)
    }

    @Test
    fun `an unknown invitation is reported as missing`() {
        val error = assertFailsWith<ApiException> { service.acceptInvitation(Uuid.generateV7().toJavaUuid(), actor()) }

        assertEquals(ProjectError.INVITATION_NOT_FOUND, error.error)
    }

    @Test
    fun `an existing member cannot accept a second invitation`() {
        val invitation = givenPendingInvitation()
        val invitee = actor(email = invitation.inviteeEmail)
        members.given(ProjectMember.create(projectId = existing.id, userId = invitee.id, roleId = memberRole.id))

        val error = assertFailsWith<ApiException> { service.acceptInvitation(invitation.id, invitee) }

        assertEquals(ProjectError.MEMBER_ALREADY_JOINED, error.error)
        assertEquals(1, members.stored.size)
    }

    @Test
    fun `rejecting closes the invitation without granting access`() {
        val invitation = givenPendingInvitation()
        val invitee = actor(email = invitation.inviteeEmail)

        val response = service.rejectInvitation(invitation.id, invitee)

        assertEquals(InvitationStatus.REJECTED, invitations.findById(invitation.id)!!.status)
        assertEquals(InvitationStatus.REJECTED, response.status)
        assertTrue(members.stored.isEmpty())
    }

    @Test
    fun `an invitation addressed to someone else cannot be rejected either`() {
        val invitation = givenPendingInvitation(email = "intended@example.com")

        val error =
            assertFailsWith<ApiException> { service.rejectInvitation(invitation.id, actor(email = "someone.else@example.com")) }

        assertEquals(ProjectError.INVITATION_NOT_ADDRESSED_TO_CALLER, error.error)
    }

    @Test
    fun `overdue invitations are expired in bulk`() {
        givenPendingInvitation(email = "a@example.com", expiresAt = Instant.now().minus(1, ChronoUnit.DAYS))
        givenPendingInvitation(email = "b@example.com", expiresAt = Instant.now().minus(2, ChronoUnit.DAYS))
        val current = givenPendingInvitation(email = "c@example.com")

        assertEquals(2, service.expireOverdueInvitations())
        assertEquals(InvitationStatus.PENDING, invitations.findById(current.id)!!.status)
    }

    @Test
    fun `an invitation that is still valid is left alone`() {
        givenPendingInvitation()

        assertEquals(0, service.expireOverdueInvitations())
    }
}

private const val NEW_EXAMPLE_COM = "new@example.com"
