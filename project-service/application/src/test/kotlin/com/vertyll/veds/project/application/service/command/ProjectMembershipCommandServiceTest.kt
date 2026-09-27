@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.ENGLISH
import com.vertyll.veds.project.application.InMemoryMemberRepository
import com.vertyll.veds.project.application.InMemoryProjectRepository
import com.vertyll.veds.project.application.InMemoryRoleRepository
import com.vertyll.veds.project.application.InMemoryUserDirectory
import com.vertyll.veds.project.application.RecordingEventPublisher
import com.vertyll.veds.project.application.command.UpdateMemberRoleCommand
import com.vertyll.veds.project.application.project
import com.vertyll.veds.project.application.role
import com.vertyll.veds.project.application.service.MemberViewAssembler
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.application.userRef
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectMembershipCommandServiceTest {
    private val projects = InMemoryProjectRepository()
    private val members = InMemoryMemberRepository()
    private val roles = InMemoryRoleRepository()
    private val users = InMemoryUserDirectory()
    private val events = RecordingEventPublisher()

    private val managerRole = role(ProjectRoleCode.MANAGER).also { roles.given(it) }
    private val memberRole =
        role(ProjectRoleCode.MEMBER, permissions = setOf(ProjectPermission.VIEW_PROJECT)).also { roles.given(it) }

    private val service =
        ProjectMembershipCommandService(
            memberRepository = members,
            roleRepository = roles,
            memberViewAssembler = MemberViewAssembler(roles, users),
            authorization = ProjectAuthorizationService(projects, members, roles),
            eventPublisher = events,
        )

    private val owner = Uuid.generateV7().toJavaUuid()
    private val existing = project(ownerId = owner).also { projects.given(it) }

    private fun givenMember(
        userId: UUID = Uuid.generateV7().toJavaUuid(),
        roleId: UUID = memberRole.id,
        projectId: UUID = existing.id,
    ): ProjectMember {
        users.given(userRef(userId = userId))
        return ProjectMember(projectId = projectId, userId = userId, roleId = roleId, version = 0L)
            .also { members.given(it) }
    }

    @Test
    fun `a member's role can be changed by someone who manages members`() {
        val member = givenMember()

        val view = service.updateMemberRole(existing.id, member.id, UpdateMemberRoleCommand(managerRole.id), owner, ENGLISH, 0L)

        assertEquals(managerRole.id, members.findById(member.id)!!.roleId)
        assertEquals(member.userId, view.userId)
    }

    @Test
    fun `changing a role announces the member's new standing`() {
        val member = givenMember()

        service.updateMemberRole(existing.id, member.id, UpdateMemberRoleCommand(managerRole.id), owner, ENGLISH, 0L)

        assertEquals(listOf("MemberJoined(${existing.id},${member.userId},MANAGER)"), events.published)
    }

    @Test
    fun `the owner's own membership cannot be reassigned`() {
        val ownerMembership = givenMember(userId = owner, roleId = managerRole.id)

        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(existing.id, ownerMembership.id, UpdateMemberRoleCommand(memberRole.id), owner, ENGLISH, 0L)
            }

        assertEquals(ProjectError.MEMBER_OWNER_IMMUTABLE, error.error)
        assertEquals(managerRole.id, members.findById(ownerMembership.id)!!.roleId)
    }

    @Test
    fun `a member of another project cannot be reached through this one`() {
        val elsewhere = givenMember(projectId = Uuid.generateV7().toJavaUuid())

        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(existing.id, elsewhere.id, UpdateMemberRoleCommand(managerRole.id), owner, ENGLISH, 0L)
            }

        assertEquals(ProjectError.MEMBER_NOT_FOUND, error.error)
    }

    @Test
    fun `an unknown member is reported as missing`() {
        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(
                    existing.id,
                    Uuid.generateV7().toJavaUuid(),
                    UpdateMemberRoleCommand(managerRole.id),
                    owner,
                    ENGLISH,
                    0L,
                )
            }

        assertEquals(ProjectError.MEMBER_NOT_FOUND, error.error)
    }

    @Test
    fun `a role that does not exist cannot be assigned`() {
        val member = givenMember()

        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(
                    existing.id,
                    member.id,
                    UpdateMemberRoleCommand(Uuid.generateV7().toJavaUuid()),
                    owner,
                    ENGLISH,
                    0L,
                )
            }

        assertEquals(ProjectError.ROLE_NOT_FOUND, error.error)
    }

    @Test
    fun `a reassignment against a stale version is refused`() {
        val member = givenMember()

        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(existing.id, member.id, UpdateMemberRoleCommand(managerRole.id), owner, ENGLISH, version = 7L)
            }

        assertEquals(ProjectError.VERSION_MISMATCH, error.error)
        assertEquals(memberRole.id, members.findById(member.id)!!.roleId)
    }

    @Test
    fun `someone without the manage-members permission cannot reassign anyone`() {
        val member = givenMember()
        val bystander = givenMember()

        assertFailsWith<ApiException> {
            service.updateMemberRole(existing.id, member.id, UpdateMemberRoleCommand(managerRole.id), bystander.userId, ENGLISH, 0L)
        }

        assertEquals(memberRole.id, members.findById(member.id)!!.roleId)
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `a member can be removed and their departure announced`() {
        val member = givenMember()

        service.removeMember(existing.id, member.id, owner)

        assertNull(members.findById(member.id))
        assertEquals(listOf("MemberRemoved(${existing.id},${member.userId})"), events.published)
    }

    @Test
    fun `the owner cannot be removed from their own project`() {
        val ownerMembership = givenMember(userId = owner, roleId = managerRole.id)

        val error = assertFailsWith<ApiException> { service.removeMember(existing.id, ownerMembership.id, owner) }

        assertEquals(ProjectError.MEMBER_OWNER_IMMUTABLE, error.error)
        assertNotNull(members.findById(ownerMembership.id))
    }

    @Test
    fun `a member of another project cannot be removed through this one`() {
        val elsewhere = givenMember(projectId = Uuid.generateV7().toJavaUuid())

        assertFailsWith<ApiException> { service.removeMember(existing.id, elsewhere.id, owner) }

        assertNotNull(members.findById(elsewhere.id))
    }

    @Test
    fun `someone without the manage-members permission cannot remove anyone`() {
        val member = givenMember()
        val bystander = givenMember()

        assertFailsWith<ApiException> { service.removeMember(existing.id, member.id, bystander.userId) }

        assertNotNull(members.findById(member.id))
        assertTrue(events.published.isEmpty())
    }
}
