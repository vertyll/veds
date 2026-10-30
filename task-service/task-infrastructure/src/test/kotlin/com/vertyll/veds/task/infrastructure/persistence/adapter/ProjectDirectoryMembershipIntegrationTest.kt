@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.infrastructure.persistence.adapter

import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class ProjectDirectoryMembershipIntegrationTest(
    private val directory: ProjectDirectoryRepository,
) : IntegrationTestBase() {
    private val projectId: UUID = Uuid.generateV7().toJavaUuid()
    private val userId: UUID = Uuid.generateV7().toJavaUuid()

    @Test
    fun `a membership is found by its project and user, and saving it again changes that one row`() {
        directory.saveMembership(ProjectMembershipRef(projectId, userId, "MEMBER"))

        directory.saveMembership(ProjectMembershipRef(projectId, userId, "OWNER"))

        assertEquals("OWNER", directory.findMembership(projectId, userId)?.roleCode)
        assertEquals(listOf(userId), directory.findMemberships(projectId).map { it.userId })
    }
}
