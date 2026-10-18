@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.ENGLISH
import com.vertyll.veds.project.application.InMemoryCategoryRepository
import com.vertyll.veds.project.application.InMemoryMemberRepository
import com.vertyll.veds.project.application.InMemoryProjectRepository
import com.vertyll.veds.project.application.InMemoryRoleRepository
import com.vertyll.veds.project.application.POLISH
import com.vertyll.veds.project.application.RecordingEventPublisher
import com.vertyll.veds.project.application.command.CreateCategoryCommand
import com.vertyll.veds.project.application.command.UpdateCategoryCommand
import com.vertyll.veds.project.application.project
import com.vertyll.veds.project.application.role
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.application.service.TranslationCompletenessValidator
import com.vertyll.veds.project.application.translation
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.ProjectCategory
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
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

internal class ProjectCategoryCommandServiceTest {
    private val categories = InMemoryCategoryRepository()
    private val projects = InMemoryProjectRepository()
    private val members = InMemoryMemberRepository()
    private val roles = InMemoryRoleRepository()
    private val events = RecordingEventPublisher()

    private val service =
        ProjectCategoryCommandService(
            categoryRepository = categories,
            authorization = ProjectAuthorizationService(projects, members, roles),
            eventPublisher = events,
            translationCompleteness = TranslationCompletenessValidator { setOf(ENGLISH, POLISH) },
        )

    private val owner = Uuid.generateV7().toJavaUuid()
    private val existing = project(ownerId = owner).also { projects.given(it) }

    private val complete: Set<Translation> = setOf(translation("Bug", ENGLISH), translation("Błąd", POLISH))

    private fun givenCategory(
        projectId: UUID = existing.id,
        isActive: Boolean = true,
    ) = ProjectCategory(projectId = projectId, color = FF0000, translations = complete, isActive = isActive, version = 0L)
        .also { categories.given(it) }

    @Test
    fun `a category is stored against its project`() {
        val response = service.createCategory(existing.id, CreateCategoryCommand(FF0000, complete), owner, ENGLISH)

        val stored = categories.findById(response.id)!!
        assertEquals(existing.id, stored.projectId)
        assertEquals(FF0000, stored.color)
    }

    @Test
    fun `the response is rendered in the language asked for`() {
        val response = service.createCategory(existing.id, CreateCategoryCommand(FF0000, complete), owner, POLISH)

        assertEquals("Błąd", response.name)
    }

    @Test
    fun `creating announces the category to other services`() {
        val response = service.createCategory(existing.id, CreateCategoryCommand(FF0000, complete), owner, ENGLISH)

        assertEquals(listOf("CategoryChanged(${existing.id},${response.id},removed=false)"), events.published)
    }

    @Test
    fun `a category missing a language is refused`() {
        val error =
            assertFailsWith<ApiException> {
                service.createCategory(existing.id, CreateCategoryCommand(FF0000, setOf(translation("Bug", ENGLISH))), owner, ENGLISH)
            }

        assertEquals(ProjectError.TRANSLATION_MISSING, error.error)
        assertTrue(categories.stored.isEmpty())
    }

    @Test
    fun `someone without edit rights cannot add a category`() {
        val viewerRole = role(ProjectRoleCode.CLIENT, permissions = setOf(ProjectPermission.VIEW_PROJECT)).also { roles.given(it) }
        val viewer = Uuid.generateV7().toJavaUuid()
        members.given(ProjectMember.create(projectId = existing.id, userId = viewer, roleId = viewerRole.id))

        assertFailsWith<ApiException> {
            service.createCategory(existing.id, CreateCategoryCommand(FF0000, complete), viewer, ENGLISH)
        }

        assertTrue(categories.stored.isEmpty())
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `updating replaces the colour and the translations`() {
        val category = givenCategory()
        val renamed = setOf(translation("Defect", ENGLISH), translation("Usterka", POLISH))

        service.updateCategory(existing.id, category.id, UpdateCategoryCommand(V_00FF00, renamed, true), owner, ENGLISH, 0L)

        val stored = categories.findById(category.id)!!
        assertEquals(V_00FF00, stored.color)
        assertEquals("Defect", stored.translationFor(ENGLISH).name)
    }

    @Test
    fun `deactivating a category is announced as a removal`() {
        val category = givenCategory()

        service.updateCategory(existing.id, category.id, UpdateCategoryCommand(FF0000, complete, isActive = false), owner, ENGLISH, 0L)

        assertTrue(!categories.findById(category.id)!!.isActive)
        assertEquals(listOf("CategoryChanged(${existing.id},${category.id},removed=true)"), events.published)
    }

    @Test
    fun `reactivating a category is announced as a change`() {
        val category = givenCategory(isActive = false)

        service.updateCategory(existing.id, category.id, UpdateCategoryCommand(FF0000, complete, isActive = true), owner, ENGLISH, 0L)

        assertEquals(listOf("CategoryChanged(${existing.id},${category.id},removed=false)"), events.published)
    }

    @Test
    fun `an update against a stale version is refused`() {
        val category = givenCategory()

        val error =
            assertFailsWith<ApiException> {
                service.updateCategory(existing.id, category.id, UpdateCategoryCommand(V_00FF00, complete, true), owner, ENGLISH, 9L)
            }

        assertEquals(ProjectError.VERSION_MISMATCH, error.error)
        assertEquals(FF0000, categories.findById(category.id)!!.color)
    }

    @Test
    fun `a category of another project cannot be reached through this one`() {
        val elsewhere = givenCategory(projectId = Uuid.generateV7().toJavaUuid())

        val error =
            assertFailsWith<ApiException> {
                service.updateCategory(existing.id, elsewhere.id, UpdateCategoryCommand(V_00FF00, complete, true), owner, ENGLISH, 0L)
            }

        assertEquals(ProjectError.CATEGORY_NOT_FOUND, error.error)
    }

    @Test
    fun `an unknown category is reported as missing`() {
        val error =
            assertFailsWith<ApiException> {
                service.updateCategory(
                    existing.id,
                    Uuid.generateV7().toJavaUuid(),
                    UpdateCategoryCommand(V_00FF00, complete, true),
                    owner,
                    ENGLISH,
                    0L,
                )
            }

        assertEquals(ProjectError.CATEGORY_NOT_FOUND, error.error)
    }

    @Test
    fun `deleting removes the category and tells other services it is gone`() {
        val category = givenCategory()

        service.deleteCategory(existing.id, category.id, owner)

        assertNull(categories.findById(category.id))
        assertEquals(listOf("CategoryChanged(${existing.id},${category.id},removed=true)"), events.published)
    }

    @Test
    fun `a category of another project cannot be deleted through this one`() {
        val elsewhere = givenCategory(projectId = Uuid.generateV7().toJavaUuid())

        assertFailsWith<ApiException> { service.deleteCategory(existing.id, elsewhere.id, owner) }

        assertEquals(1, categories.stored.size)
    }
}

private const val FF0000 = "#ff0000"
private const val V_00FF00 = "#00ff00"
