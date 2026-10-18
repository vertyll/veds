@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.InMemoryMemberRepository
import com.vertyll.veds.project.application.InMemoryProjectRepository
import com.vertyll.veds.project.application.InMemoryRoleRepository
import com.vertyll.veds.project.application.InMemoryTypeRepository
import com.vertyll.veds.project.application.InMemoryUserDirectory
import com.vertyll.veds.project.application.RecordingEventPublisher
import com.vertyll.veds.project.application.actor
import com.vertyll.veds.project.application.command.CreateProjectCommand
import com.vertyll.veds.project.application.command.UpdateProjectCommand
import com.vertyll.veds.project.application.project
import com.vertyll.veds.project.application.projectType
import com.vertyll.veds.project.application.role
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
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
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectCommandServiceTest {
    private val projects = InMemoryProjectRepository()
    private val members = InMemoryMemberRepository()
    private val roles = InMemoryRoleRepository()
    private val types = InMemoryTypeRepository()
    private val users = InMemoryUserDirectory()
    private val events = RecordingEventPublisher()

    private val managerRole = role(ProjectRoleCode.MANAGER)

    private val service =
        ProjectCommandService(
            projectRepository = projects,
            memberRepository = members,
            roleRepository = roles,
            typeRepository = types,
            userDirectory = users,
            authorization = ProjectAuthorizationService(projects, members, roles),
            eventPublisher = events,
        )

    private val creator = actor()

    init {
        roles.given(managerRole)
    }

    private fun createCommand(typeId: UUID? = null) =
        CreateProjectCommand(
            name = "Apollo",
            description = "Moon landing",
            isPublic = false,
            typeId = typeId,
            iconFileId = null,
        )

    @Test
    fun `a created project belongs to whoever created it`() {
        val response = service.createProject(createCommand(), creator)

        val saved = projects.findById(response.id)
        assertNotNull(saved)
        assertEquals("Apollo", saved.name)
        assertEquals(creator.id, saved.ownerId)
    }

    @Test
    fun `the creator is enrolled as a manager`() {
        val response = service.createProject(createCommand(), creator)

        val membership = members.findByProjectIdAndUserId(response.id, creator.id)
        assertNotNull(membership)
        assertEquals(managerRole.id, membership.roleId)
    }

    @Test
    fun `the creator is recorded in the user directory`() {
        service.createProject(createCommand(), creator)

        val known = users.findById(creator.id)
        assertNotNull(known)
        assertEquals(creator.email, known.email)
    }

    @Test
    fun `creating a project announces it`() {
        val response = service.createProject(createCommand(), creator)

        assertEquals("ProjectCreated(${response.id})", events.published.first())
    }

    @Test
    fun `creating a project announces the creator joining it`() {
        val response = service.createProject(createCommand(), creator)

        assertEquals(
            listOf("ProjectCreated(${response.id})", "MemberJoined(${response.id},${creator.id},MANAGER)"),
            events.published,
        )
    }

    @Test
    fun `an unknown project type is rejected`() {
        val error = assertFailsWith<ApiException> { service.createProject(createCommand(typeId = Uuid.generateV7().toJavaUuid()), creator) }

        assertEquals(ProjectError.TYPE_NOT_FOUND, error.error)
    }

    @Test
    fun `a project with an unknown type is not saved`() {
        runCatching { service.createProject(createCommand(typeId = Uuid.generateV7().toJavaUuid()), creator) }

        assertTrue(projects.stored.isEmpty())
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `a known project type is accepted`() {
        val type = projectType()
        types.given(type)

        val response = service.createProject(createCommand(typeId = type.id), creator)

        assertEquals(type.id, projects.findById(response.id)!!.typeId)
    }

    @Test
    fun `a missing manager role is reported rather than worked around`() {
        roles.stored.clear()

        val error = assertFailsWith<ApiException> { service.createProject(createCommand(), creator) }

        assertEquals(ProjectError.ROLE_NOT_CONFIGURED, error.error)
    }

    private fun givenManagedProject(version: Long? = 0L) =
        project(ownerId = creator.id, version = version)
            .also {
                projects.given(it)
                members.given(ProjectMember.create(projectId = it.id, userId = creator.id, roleId = managerRole.id))
            }

    @Test
    fun `updating replaces every editable field`() {
        val existing = givenManagedProject()

        val updated =
            service.updateProject(
                projectId = existing.id,
                command = UpdateProjectCommand(name = "Artemis", description = "Return", isPublic = true, typeId = null, iconFileId = null),
                actorId = creator.id,
                version = 0L,
            )

        val saved = projects.findById(existing.id)!!
        assertEquals("Artemis", saved.name)
        assertEquals("Return", saved.description)
        assertTrue(saved.isPublic)
        assertEquals("Artemis", updated.name)
    }

    @Test
    fun `an update against a stale version is refused`() {
        val existing = givenManagedProject(version = 3L)

        val error =
            assertFailsWith<ApiException> {
                service.updateProject(
                    projectId = existing.id,
                    command = UpdateProjectCommand("Artemis", null, false, null, null),
                    actorId = creator.id,
                    version = 1L,
                )
            }

        assertEquals(ProjectError.VERSION_MISMATCH, error.error)
        assertEquals("Apollo", projects.findById(existing.id)!!.name)
    }

    @Test
    fun `someone without edit rights cannot update`() {
        val existing = givenManagedProject()
        val outsider = Uuid.generateV7().toJavaUuid()

        val error =
            assertFailsWith<ApiException> {
                service.updateProject(existing.id, UpdateProjectCommand("Artemis", null, false, null, null), outsider, 0L)
            }

        assertEquals(ProjectError.PROJECT_NOT_FOUND, error.error, "a private project must not reveal that it exists")
    }

    @Test
    fun `archiving deactivates the project and announces it`() {
        val existing = givenManagedProject()

        service.archiveProject(existing.id, creator.id, version = 0L)

        assertTrue(!projects.findById(existing.id)!!.isActive)
        assertEquals(listOf("ProjectArchived(${existing.id})"), events.published)
    }

    @Test
    fun `archiving an already archived project is refused`() {
        val existing =
            project(ownerId = creator.id, isActive = false).also {
                projects.given(it)
                members.given(ProjectMember.create(projectId = it.id, userId = creator.id, roleId = managerRole.id))
            }

        val error = assertFailsWith<ApiException> { service.archiveProject(existing.id, creator.id, version = 0L) }

        assertEquals(ProjectError.PROJECT_ARCHIVED, error.error)
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `archiving requires the delete permission`() {
        val existing = project(ownerId = Uuid.generateV7().toJavaUuid())
        val viewerRole = role(ProjectRoleCode.CLIENT, permissions = setOf(ProjectPermission.VIEW_PROJECT))
        roles.given(viewerRole)
        projects.given(existing)
        members.given(ProjectMember.create(projectId = existing.id, userId = creator.id, roleId = viewerRole.id))

        assertFailsWith<ApiException> { service.archiveProject(existing.id, creator.id, version = 0L) }

        assertTrue(projects.findById(existing.id)!!.isActive)
    }
}
