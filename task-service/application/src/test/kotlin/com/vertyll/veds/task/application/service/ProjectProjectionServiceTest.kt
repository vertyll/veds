@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.InMemoryProjectDirectory
import com.vertyll.veds.task.application.InMemoryTaskRepository
import com.vertyll.veds.task.application.SilentLogger
import com.vertyll.veds.task.application.categoryRef
import com.vertyll.veds.task.application.membership
import com.vertyll.veds.task.application.projectRef
import com.vertyll.veds.task.application.statusRef
import com.vertyll.veds.task.application.task
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectProjectionServiceTest {
    private val directory = InMemoryProjectDirectory()
    private val tasks = InMemoryTaskRepository()

    private val service = ProjectProjectionService(directory, tasks, SilentLogger)

    private val projectId = Uuid.generateV7().toJavaUuid()

    @Test
    fun `a project change is recorded locally`() {
        service.projectChanged(projectRef(projectId, name = "Apollo"))

        assertEquals("Apollo", directory.findProject(projectId)!!.name)
    }

    @Test
    fun `a later change replaces the earlier copy`() {
        service.projectChanged(projectRef(projectId, name = "Apollo"))
        service.projectChanged(projectRef(projectId, name = "Artemis"))

        assertEquals("Artemis", directory.findProject(projectId)!!.name)
        assertEquals(1, directory.projects.size)
    }

    @Test
    fun `archiving marks the local copy inactive rather than deleting it`() {
        service.projectChanged(projectRef(projectId))

        service.projectArchived(projectId)

        val stored = directory.findProject(projectId)!!
        assertTrue(!stored.isActive, "tasks still reference the project, so the row has to stay")
    }

    @Test
    fun `archiving a project that was never projected is ignored`() {
        service.projectArchived(Uuid.generateV7().toJavaUuid())

        assertTrue(directory.projects.isEmpty())
    }

    @Test
    fun `a category change is recorded locally`() {
        val category = categoryRef(projectId, name = "Bug")

        service.categoryChanged(category)

        assertEquals(listOf(category), directory.findCategories(projectId))
    }

    @Test
    fun `removing a category also strips it from the tasks carrying it`() {
        val category = categoryRef(projectId)
        val other = Uuid.generateV7().toJavaUuid()
        service.categoryChanged(category)
        val labelled = task(projectId, categoryIds = setOf(category.categoryId, other)).also { tasks.given(it) }

        service.categoryRemoved(category.categoryId)

        assertEquals(setOf(other), tasks.findById(labelled.id)!!.categoryIds)
        assertTrue(directory.findCategories(projectId).isEmpty())
    }

    @Test
    fun `removing a category twice is harmless`() {
        val category = categoryRef(projectId)
        service.categoryChanged(category)
        val labelled = task(projectId, categoryIds = setOf(category.categoryId)).also { tasks.given(it) }

        service.categoryRemoved(category.categoryId)
        service.categoryRemoved(category.categoryId)

        assertTrue(tasks.findById(labelled.id)!!.categoryIds.isEmpty())
    }

    @Test
    fun `tasks without the removed category are left alone`() {
        val category = categoryRef(projectId)
        val untouched = task(projectId).also { tasks.given(it) }

        service.categoryRemoved(category.categoryId)

        assertEquals(untouched, tasks.findById(untouched.id))
    }

    @Test
    fun `a status change is recorded locally`() {
        val status = statusRef(projectId, name = "In progress")

        service.statusChanged(status)

        assertEquals(listOf(status), directory.findStatuses(projectId))
    }

    @Test
    fun `removing a status clears it from the tasks sitting in it`() {
        val status = statusRef(projectId)
        service.statusChanged(status)
        val sitting = task(projectId, statusId = status.statusId).also { tasks.given(it) }

        service.statusRemoved(status.statusId)

        assertNull(tasks.findById(sitting.id)!!.statusId)
        assertTrue(directory.findStatuses(projectId).isEmpty())
    }

    @Test
    fun `removing a status twice is harmless`() {
        val status = statusRef(projectId)
        val sitting = task(projectId, statusId = status.statusId).also { tasks.given(it) }

        service.statusRemoved(status.statusId)
        service.statusRemoved(status.statusId)

        assertNull(tasks.findById(sitting.id)!!.statusId)
    }

    @Test
    fun `a member joining is recorded locally`() {
        val joined = membership(projectId, roleCode = "MANAGER")

        service.memberJoined(joined)

        assertEquals(joined, directory.findMembership(projectId, joined.userId))
    }

    @Test
    fun `a role change replaces the existing membership`() {
        val userId = Uuid.generateV7().toJavaUuid()
        service.memberJoined(membership(projectId, userId, roleCode = "MEMBER"))
        service.memberJoined(membership(projectId, userId, roleCode = "MANAGER"))

        assertEquals("MANAGER", directory.findMembership(projectId, userId)!!.roleCode)
        assertEquals(1, directory.findMemberships(projectId).size)
    }

    @Test
    fun `a member being removed loses their local membership`() {
        val joined = membership(projectId)
        service.memberJoined(joined)

        service.memberRemoved(projectId, joined.userId)

        assertNull(directory.findMembership(projectId, joined.userId))
    }

    @Test
    fun `removing a member twice is harmless`() {
        val joined = membership(projectId)
        service.memberJoined(joined)

        service.memberRemoved(projectId, joined.userId)
        service.memberRemoved(projectId, joined.userId)

        assertTrue(directory.findMemberships(projectId).isEmpty())
    }

    @Test
    fun `removing a member leaves the other members of the project`() {
        val leaving = membership(projectId)
        val staying = membership(projectId)
        service.memberJoined(leaving)
        service.memberJoined(staying)

        service.memberRemoved(projectId, leaving.userId)

        assertEquals(listOf(staying), directory.findMemberships(projectId))
    }
}
