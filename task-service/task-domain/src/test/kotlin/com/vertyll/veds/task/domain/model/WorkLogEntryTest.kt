@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.domain.model

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class WorkLogEntryTest {
    private val author = Uuid.generateV7().toJavaUuid()
    private val someoneElse = Uuid.generateV7().toJavaUuid()

    private val member = RolePermissionsRef(roleName = "MEMBER", permissions = setOf(TaskPermission.LOG_WORK.name))
    private val manager =
        RolePermissionsRef(
            roleName = "MANAGER",
            permissions = setOf(TaskPermission.LOG_WORK.name, TaskPermission.VIEW_HIDDEN_WORK_LOG.name),
        )

    private fun entry(hidden: Boolean) =
        WorkLogEntry.create(
            taskId = Uuid.generateV7().toJavaUuid(),
            authorId = author,
            minutes = 60,
            workedOn = LocalDate.of(2026, 9, 2),
            hidden = hidden,
        )

    @Test
    fun `a normal entry is visible to everyone who may see the task`() {
        assertTrue(entry(hidden = false).isVisibleTo(someoneElse, member))
    }

    @Test
    fun `a hidden entry stays visible to its own author`() {
        assertTrue(entry(hidden = true).isVisibleTo(author, member))
    }

    @Test
    fun `a hidden entry is invisible to a role without the grant`() {
        assertFalse(entry(hidden = true).isVisibleTo(someoneElse, member))
    }

    @Test
    fun `a hidden entry opens up to a role granted the right to read it`() {
        assertTrue(entry(hidden = true).isVisibleTo(someoneElse, manager))
    }

    @Test
    fun `an unrestricted role reads hidden entries without being granted them one by one`() {
        val administrator = RolePermissionsRef(roleName = "ADMIN", unrestricted = true)

        assertTrue(entry(hidden = true).isVisibleTo(someoneElse, administrator))
    }

    @Test
    fun `somebody holding no role in the project never reads hidden entries`() {
        assertFalse(entry(hidden = true).isVisibleTo(someoneElse, null))
    }
}
