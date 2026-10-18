@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.InMemoryMemberRepository
import com.vertyll.veds.project.application.InMemoryProjectRepository
import com.vertyll.veds.project.application.InMemoryRoleRepository
import com.vertyll.veds.project.application.project
import com.vertyll.veds.project.application.role
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectAuthorizationServiceTest {
    private val projects = InMemoryProjectRepository()
    private val members = InMemoryMemberRepository()
    private val roles = InMemoryRoleRepository()

    private val service = ProjectAuthorizationService(projects, members, roles)

    private val owner = Uuid.generateV7().toJavaUuid()
    private val outsider = Uuid.generateV7().toJavaUuid()

    private fun givenMember(
        projectId: UUID,
        userId: UUID,
        permissions: Set<ProjectPermission>,
    ) {
        val memberRole = role(ProjectRoleCode.MEMBER, permissions = permissions)
        roles.given(memberRole)
        members.given(ProjectMember.create(projectId = projectId, userId = userId, roleId = memberRole.id))
    }

    @Test
    fun `an owner may do anything on their own project`() {
        val existing = project(ownerId = owner).also { projects.given(it) }

        ProjectPermission.entries.forEach { permission ->
            assertEquals(existing.id, service.requirePermission(existing.id, owner, permission).id)
        }
    }

    @Test
    fun `a private project is indistinguishable from a missing one`() {
        val existing = project(ownerId = owner, isPublic = false).also { projects.given(it) }

        val onExisting =
            assertFailsWith<ApiException> { service.requirePermission(existing.id, outsider, ProjectPermission.VIEW_PROJECT) }
        val onMissing =
            assertFailsWith<ApiException> {
                service.requirePermission(
                    Uuid.generateV7().toJavaUuid(),
                    outsider,
                    ProjectPermission.VIEW_PROJECT,
                )
            }

        assertEquals(ProjectError.PROJECT_NOT_FOUND, onExisting.error)
        assertEquals(onMissing.error, onExisting.error)
    }

    @Test
    fun `anyone may view a public project`() {
        val existing = project(ownerId = owner, isPublic = true).also { projects.given(it) }

        assertEquals(existing.id, service.requirePermission(existing.id, outsider, ProjectPermission.VIEW_PROJECT).id)
    }

    @Test
    fun `a viewer denied a stronger permission is told access was denied`() {
        val existing = project(ownerId = owner, isPublic = true).also { projects.given(it) }

        val error = assertFailsWith<ApiException> { service.requirePermission(existing.id, outsider, ProjectPermission.EDIT_PROJECT) }

        assertEquals(ProjectError.PROJECT_ACCESS_DENIED, error.error)
    }

    @Test
    fun `a member is granted exactly what their role carries`() {
        val existing = project(ownerId = owner).also { projects.given(it) }
        givenMember(existing.id, outsider, setOf(ProjectPermission.VIEW_PROJECT, ProjectPermission.INVITE_USERS))

        service.requirePermission(existing.id, outsider, ProjectPermission.INVITE_USERS)

        val error = assertFailsWith<ApiException> { service.requirePermission(existing.id, outsider, ProjectPermission.EDIT_PROJECT) }
        assertEquals(ProjectError.PROJECT_ACCESS_DENIED, error.error)
    }

    @Test
    fun `an archived project refuses every change, even from its owner`() {
        val existing = project(ownerId = owner, isActive = false).also { projects.given(it) }

        val error = assertFailsWith<ApiException> { service.requirePermission(existing.id, owner, ProjectPermission.EDIT_PROJECT) }

        assertEquals(ProjectError.PROJECT_ARCHIVED, error.error)
        assertEquals(existing.id, service.requirePermission(existing.id, owner, ProjectPermission.VIEW_PROJECT).id)
    }

    @Test
    fun `a missing project is reported before anything else is checked`() {
        val error =
            assertFailsWith<ApiException> {
                service.requirePermission(
                    Uuid.generateV7().toJavaUuid(),
                    owner,
                    ProjectPermission.VIEW_PROJECT,
                )
            }

        assertEquals(ProjectError.PROJECT_NOT_FOUND, error.error)
    }

    @Test
    fun `an owner's effective permissions cover everything`() {
        val existing = project(ownerId = owner).also { projects.given(it) }

        assertEquals(
            ProjectPermission.entries.mapTo(mutableSetOf()) { it.name },
            service.effectivePermissions(existing.id, owner),
        )
    }

    @Test
    fun `an outsider on a public project may only view it`() {
        val existing = project(ownerId = owner, isPublic = true).also { projects.given(it) }

        assertEquals(setOf(ProjectPermission.VIEW_PROJECT.name), service.effectivePermissions(existing.id, outsider))
    }

    @Test
    fun `an outsider on a private project has none`() {
        val existing = project(ownerId = owner).also { projects.given(it) }

        assertTrue(service.effectivePermissions(existing.id, outsider).isEmpty())
    }

    @Test
    fun `an archived project leaves its owner read-only`() {
        val existing = project(ownerId = owner, isActive = false).also { projects.given(it) }

        val effective = service.effectivePermissions(existing.id, owner)

        assertTrue(ProjectPermission.EDIT_PROJECT.name !in effective)
        assertTrue(ProjectPermission.VIEW_PROJECT.name in effective)
    }

    @Test
    fun `asking about a project that does not exist is an error`() {
        assertFailsWith<ApiException> { service.effectivePermissions(Uuid.generateV7().toJavaUuid(), owner) }
    }
}
