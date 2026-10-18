package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.ENGLISH
import com.vertyll.veds.project.application.POLISH
import com.vertyll.veds.project.application.translation
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TranslationCompletenessValidatorTest {
    private val validator = TranslationCompletenessValidator { setOf(ENGLISH, POLISH) }

    @Test
    fun `a translation for every supported language is accepted`() {
        validator.validate(setOf(translation("Bug", ENGLISH), translation("Błąd", POLISH)))
    }

    @Test
    fun `a missing language is refused`() {
        val error = assertFailsWith<ApiException> { validator.validate(setOf(translation("Bug", ENGLISH))) }

        assertEquals(ProjectError.TRANSLATION_MISSING, error.error)
    }

    @Test
    fun `the refusal names the languages that are missing`() {
        val error = assertFailsWith<ApiException> { validator.validate(setOf(translation("Bug", ENGLISH))) }

        assertEquals(listOf("pl"), error.params["missing"])
    }

    @Test
    fun `a language the deployment does not serve is refused`() {
        val german = LanguageTag("de")

        val error =
            assertFailsWith<ApiException> {
                validator.validate(setOf(translation("Bug", ENGLISH), translation("Błąd", POLISH), translation("Fehler", german)))
            }

        assertEquals(ProjectError.LANGUAGE_NOT_SUPPORTED, error.error)
        assertEquals(listOf("de"), error.params["unsupported"])
    }

    @Test
    fun `a gap is reported before an unknown language`() {
        val error =
            assertFailsWith<ApiException> {
                validator.validate(setOf(translation("Bug", ENGLISH), translation("Fehler", LanguageTag("de"))))
            }

        assertEquals(ProjectError.TRANSLATION_MISSING, error.error)
    }

    @Test
    fun `no translations at all is refused`() {
        val error = assertFailsWith<ApiException> { validator.validate(emptySet()) }

        assertEquals(ProjectError.TRANSLATION_MISSING, error.error)
        assertEquals(listOf("en", "pl"), error.params["missing"])
    }
}
