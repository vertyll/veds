@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.infrastructure.persistence.query

import com.vertyll.veds.project.application.command.CreateCategoryCommand
import com.vertyll.veds.project.application.command.CreateProjectCommand
import com.vertyll.veds.project.application.command.CreateStatusCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.port.inbound.command.ProjectCategoryCommandUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectCommandUseCase
import com.vertyll.veds.project.application.port.inbound.command.ProjectStatusCommandUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectQueryPort
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.Translation
import com.vertyll.veds.project.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectQueryAdapterIntegrationTest(
    private val projectQueries: ProjectQueryPort,
    private val projectCommands: ProjectCommandUseCase,
    private val categoryCommands: ProjectCategoryCommandUseCase,
    private val statusCommands: ProjectStatusCommandUseCase,
) : IntegrationTestBase() {
    private val owner =
        Actor(
            id = Uuid.generateV7().toJavaUuid(),
            email = "owner@example.com",
            firstName = "Ada",
            lastName = "Lovelace",
        )

    private fun project() =
        projectCommands.createProject(
            CreateProjectCommand(
                name = "Apollo",
                description = "To the moon",
                isPublic = false,
                typeId = null,
                iconFileId = null,
            ),
            owner,
        )

    @Test
    fun `a project is read back with its owner`() {
        val created = project()

        val found = projectQueries.findProject(created.id)

        assertNotNull(found)
        assertEquals("Apollo", found.name)
        assertEquals("To the moon", found.description)
        assertEquals(owner.id, found.ownerId)
    }

    @Test
    fun `a project that does not exist is not found`() {
        assertNull(projectQueries.findProject(UUID.randomUUID()))
    }

    @Test
    fun `the owner is listed as a member, with the role named in the language asked for`() {
        val created = project()

        val member = projectQueries.findMembers(created.id, POLISH).single()

        assertEquals(owner.id, member.userId)
        assertEquals(owner.email, member.email)
        assertEquals("Ada Lovelace", member.displayName)
        assertEquals(created.id, member.projectId)
    }

    @Test
    fun `a project's categories are named in the language asked for`() {
        val created = project()
        categoryCommands.createCategory(created.id, CreateCategoryCommand("#FF0000", names("Błąd", "Bug")), owner.id, POLISH)

        val category = projectQueries.findCategories(created.id, POLISH).single()

        assertEquals("Błąd", category.name)
        assertEquals("#FF0000", category.color)
    }

    @Test
    fun `a project's statuses are named in the language asked for`() {
        val created = project()
        statusCommands.createStatus(created.id, CreateStatusCommand("#00FF00", names("W toku", "In progress")), owner.id, POLISH)

        val status = projectQueries.findStatuses(created.id, LanguageTag("en")).single()

        assertEquals("In progress", status.name)
        assertEquals("#00FF00", status.color)
    }

    private fun names(
        polish: String,
        english: String,
    ) = setOf(
        Translation(language = POLISH, name = polish, description = null),
        Translation(language = LanguageTag("en"), name = english, description = null),
    )

    private companion object {
        private val POLISH = LanguageTag("pl")
    }
}
