@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.ENGLISH
import com.vertyll.veds.project.application.InMemoryMemberRepository
import com.vertyll.veds.project.application.InMemoryProjectRepository
import com.vertyll.veds.project.application.InMemoryRoleRepository
import com.vertyll.veds.project.application.InMemoryStatusRepository
import com.vertyll.veds.project.application.POLISH
import com.vertyll.veds.project.application.RecordingEventPublisher
import com.vertyll.veds.project.application.command.CreateStatusCommand
import com.vertyll.veds.project.application.command.UpdateStatusCommand
import com.vertyll.veds.project.application.project
import com.vertyll.veds.project.application.role
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.application.service.TranslationCompletenessValidator
import com.vertyll.veds.project.application.translation
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.model.ProjectStatus
import com.vertyll.veds.project.domain.model.Translation
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectStatusCommandServiceTest {
    private val statuses = InMemoryStatusRepository()
    private val projects = InMemoryProjectRepository()
    private val members = InMemoryMemberRepository()
    private val roles = InMemoryRoleRepository()
    private val events = RecordingEventPublisher()

    private val service =
        ProjectStatusCommandService(
            statusRepository = statuses,
            authorization = ProjectAuthorizationService(projects, members, roles),
            eventPublisher = events,
            translationCompleteness = TranslationCompletenessValidator { setOf(ENGLISH, POLISH) },
        )

    private val owner = Uuid.generateV7().toJavaUuid()
    private val existing = project(ownerId = owner).also { projects.given(it) }

    private val complete: Set<Translation> = setOf(translation("In progress", ENGLISH), translation("W toku", POLISH))

    private fun givenStatus(
        projectId: UUID = existing.id,
        isActive: Boolean = true,
    ) = ProjectStatus(projectId = projectId, color = FF0000, translations = complete, isActive = isActive, version = 0L)
        .also { statuses.given(it) }

    @Test
    fun `a status is stored against its project`() {
        val response = service.createStatus(existing.id, CreateStatusCommand(FF0000, complete), owner, ENGLISH)

        val stored = statuses.findById(response.id)!!
        assertEquals(existing.id, stored.projectId)
        assertEquals(FF0000, stored.color)
    }

    @Test
    fun `the response is rendered in the language asked for`() {
        val response = service.createStatus(existing.id, CreateStatusCommand(FF0000, complete), owner, POLISH)

        assertEquals("W toku", response.name)
    }

    @Test
    fun `creating announces the status to other services`() {
        val response = service.createStatus(existing.id, CreateStatusCommand(FF0000, complete), owner, ENGLISH)

        assertEquals(listOf("StatusChanged(${existing.id},${response.id},removed=false)"), events.published)
    }

    @Test
    fun `a status missing a language is refused`() {
        val error =
            assertFailsWith<ApiException> {
                service.createStatus(
                    existing.id,
                    CreateStatusCommand(FF0000, setOf(translation("In progress", ENGLISH))),
                    owner,
                    ENGLISH,
                )
            }

        assertEquals(ProjectError.TRANSLATION_MISSING, error.error)
        assertTrue(statuses.stored.isEmpty())
    }

    @Test
    fun `someone without edit rights cannot add a status`() {
        val viewerRole = role(ProjectRoleCode.CLIENT, permissions = setOf(ProjectPermission.VIEW_PROJECT)).also { roles.given(it) }
        val viewer = Uuid.generateV7().toJavaUuid()
        members.given(ProjectMember.create(projectId = existing.id, userId = viewer, roleId = viewerRole.id))

        assertFailsWith<ApiException> {
            service.createStatus(existing.id, CreateStatusCommand(FF0000, complete), viewer, ENGLISH)
        }

        assertTrue(statuses.stored.isEmpty())
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `updating replaces the colour and the translations`() {
        val status = givenStatus()
        val renamed = setOf(translation("Blocked", ENGLISH), translation("Zablokowane", POLISH))

        service.updateStatus(existing.id, status.id, UpdateStatusCommand(V_00FF00, renamed, true), owner, ENGLISH, 0L)

        val stored = statuses.findById(status.id)!!
        assertEquals(V_00FF00, stored.color)
        assertEquals("Blocked", stored.translationFor(ENGLISH).name)
    }

    @Test
    fun `deactivating a status is announced as a removal`() {
        val status = givenStatus()

        service.updateStatus(existing.id, status.id, UpdateStatusCommand(FF0000, complete, isActive = false), owner, ENGLISH, 0L)

        assertTrue(!statuses.findById(status.id)!!.isActive)
        assertEquals(listOf("StatusChanged(${existing.id},${status.id},removed=true)"), events.published)
    }

    @Test
    fun `reactivating a status is announced as a change`() {
        val status = givenStatus(isActive = false)

        service.updateStatus(existing.id, status.id, UpdateStatusCommand(FF0000, complete, isActive = true), owner, ENGLISH, 0L)

        assertEquals(listOf("StatusChanged(${existing.id},${status.id},removed=false)"), events.published)
    }

    @Test
    fun `an update against a stale version is refused`() {
        val status = givenStatus()

        val error =
            assertFailsWith<ApiException> {
                service.updateStatus(existing.id, status.id, UpdateStatusCommand(V_00FF00, complete, true), owner, ENGLISH, 9L)
            }

        assertEquals(ProjectError.VERSION_MISMATCH, error.error)
        assertEquals(FF0000, statuses.findById(status.id)!!.color)
    }

    @Test
    fun `a status of another project cannot be reached through this one`() {
        val elsewhere = givenStatus(projectId = Uuid.generateV7().toJavaUuid())

        val error =
            assertFailsWith<ApiException> {
                service.updateStatus(existing.id, elsewhere.id, UpdateStatusCommand(V_00FF00, complete, true), owner, ENGLISH, 0L)
            }

        assertEquals(ProjectError.STATUS_NOT_FOUND, error.error)
    }

    @Test
    fun `an unknown status is reported as missing`() {
        val error =
            assertFailsWith<ApiException> {
                service.updateStatus(
                    existing.id,
                    Uuid.generateV7().toJavaUuid(),
                    UpdateStatusCommand(V_00FF00, complete, true),
                    owner,
                    ENGLISH,
                    0L,
                )
            }

        assertEquals(ProjectError.STATUS_NOT_FOUND, error.error)
    }

    @Test
    fun `deleting removes the status and tells other services it is gone`() {
        val status = givenStatus()

        service.deleteStatus(existing.id, status.id, owner)

        assertNull(statuses.findById(status.id))
        assertEquals(listOf("StatusChanged(${existing.id},${status.id},removed=true)"), events.published)
    }

    @Test
    fun `a status of another project cannot be deleted through this one`() {
        val elsewhere = givenStatus(projectId = Uuid.generateV7().toJavaUuid())

        assertFailsWith<ApiException> { service.deleteStatus(existing.id, elsewhere.id, owner) }

        assertEquals(1, statuses.stored.size)
    }
}

private const val FF0000 = "#ff0000"
private const val V_00FF00 = "#00ff00"
