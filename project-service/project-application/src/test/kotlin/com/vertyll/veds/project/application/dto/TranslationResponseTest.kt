@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.application.ENGLISH
import com.vertyll.veds.project.application.POLISH
import com.vertyll.veds.project.domain.model.ProjectCategory
import com.vertyll.veds.project.domain.model.ProjectStatus
import com.vertyll.veds.project.domain.model.Translation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class TranslationResponseTest {
    private val projectId = Uuid.generateV7().toJavaUuid()

    private val translations =
        setOf(
            Translation(language = ENGLISH, name = "Bug", description = "Something is broken"),
            Translation(language = POLISH, name = "Błąd"),
        )

    private val expected =
        setOf(
            TranslationResponse(language = "en", name = "Bug", description = "Something is broken"),
            TranslationResponse(language = "pl", name = "Błąd", description = null),
        )

    @Test
    fun `a translation carries its language as the plain tag`() {
        val response = TranslationResponse.from(Translation(language = POLISH, name = "Błąd", description = "Opis"))

        assertEquals(TranslationResponse(language = "pl", name = "Błąd", description = "Opis"), response)
    }

    @Test
    fun `a category response carries every translation`() {
        val category = ProjectCategory(projectId = projectId, color = "#ff0000", translations = translations)

        assertEquals(expected, ProjectCategoryResponse.from(category, ENGLISH).translations)
    }

    @Test
    fun `a status response carries every translation`() {
        val status = ProjectStatus(projectId = projectId, color = "#ff0000", translations = translations)

        assertEquals(expected, ProjectStatusResponse.from(status, POLISH).translations)
    }
}
