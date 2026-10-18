@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.template.application.service

import com.vertyll.veds.template.application.InMemoryTemplateRepository
import com.vertyll.veds.template.application.SilentLogger
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.domain.model.Template
import org.junit.jupiter.api.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class TemplateCompensationServiceTest {
    private val templates = InMemoryTemplateRepository()
    private val service = TemplateCompensationService(templates, SilentLogger)

    private fun givenTemplate() = Template(name = "welcome", payload = "{}").also { templates.given(it) }

    @Test
    fun `a template from a failed workflow is deleted`() {
        val created = givenTemplate()

        service.compensate(TemplateCompensationCommand.DeleteTemplate(created.id))

        assertNull(templates.findById(created.id))
    }

    @Test
    fun `deleting the same template twice is harmless`() {
        val created = givenTemplate()
        val command = TemplateCompensationCommand.DeleteTemplate(created.id)

        service.compensate(command)
        service.compensate(command)

        assertTrue(templates.stored.isEmpty())
    }

    @Test
    fun `a template that no longer exists is not an error`() {
        service.compensate(TemplateCompensationCommand.DeleteTemplate(Uuid.generateV7().toString()))

        assertTrue(templates.stored.isEmpty())
    }

    @Test
    fun `a published event has no rollback and leaves the template alone`() {
        val created = givenTemplate()

        service.compensate(TemplateCompensationCommand.LogTemplateCompensation(created.id))

        assertTrue(templates.stored.containsKey(created.id))
    }
}
