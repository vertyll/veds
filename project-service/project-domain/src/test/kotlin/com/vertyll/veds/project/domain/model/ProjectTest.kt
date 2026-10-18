@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class ProjectTest {
    private val ownerId = Uuid.generateV7().toJavaUuid()

    private fun project() = Project.create("Apollo", null, false, null, ownerId)

    @Test
    fun `assigns its own identity so events can reference it before commit`() {
        val a = project()
        val b = project()

        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `rejects a blank name`() {
        assertFailsWith<IllegalArgumentException> { Project.create("  ", null, false, null, ownerId) }
    }

    @Test
    fun `rejects a name over the column length`() {
        assertFailsWith<IllegalArgumentException> {
            Project.create("x".repeat(256), null, false, null, ownerId)
        }
    }

    @Test
    fun `archives without losing the record`() {
        val archived = project().archive()

        assertFalse(archived.isActive)
        assertTrue(archived.restore().isActive)
    }

    @Test
    fun `keeps its identity across modifications`() {
        val original = project()
        val renamed = original.rename("Artemis")

        assertEquals(original.id, renamed.id)
        assertEquals("Artemis", renamed.name)
    }

    @Test
    fun `recognises its owner`() {
        val project = project()

        assertTrue(project.isOwnedBy(ownerId))
        assertFalse(project.isOwnedBy(Uuid.generateV7().toJavaUuid()))
    }
}
