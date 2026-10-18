@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.ENGLISH
import com.vertyll.veds.project.application.InMemoryRoleRepository
import com.vertyll.veds.project.application.InMemoryUserDirectory
import com.vertyll.veds.project.application.role
import com.vertyll.veds.project.application.userRef
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class MemberViewAssemblerTest {
    private val roles = InMemoryRoleRepository()
    private val users = InMemoryUserDirectory()

    private val assembler = MemberViewAssembler(roles, users)

    private val projectId = Uuid.generateV7().toJavaUuid()
    private val memberRole = role(ProjectRoleCode.MEMBER).also { roles.given(it) }

    private fun member(userId: UUID) = ProjectMember.create(projectId = projectId, userId = userId, roleId = memberRole.id)

    @Test
    fun `an empty membership list needs no lookups`() {
        assertTrue(assembler.assemble(emptyList(), ENGLISH).isEmpty())
    }

    @Test
    fun `each member is named from the user directory`() {
        val user = userRef(email = "grace@example.com").also { users.given(it) }

        val view = assembler.assemble(listOf(member(user.userId)), ENGLISH).single()

        assertEquals(user.userId, view.userId)
        assertEquals("grace@example.com", view.email)
    }

    @Test
    fun `the member list preserves the order it was given`() {
        val first = userRef(email = "a@example.com").also { users.given(it) }
        val second = userRef(email = "b@example.com").also { users.given(it) }

        val view = assembler.assemble(listOf(member(second.userId), member(first.userId)), ENGLISH)

        assertEquals(listOf(second.userId, first.userId), view.map { it.userId })
    }

    @Test
    fun `a member the directory does not know is an error`() {
        val error = assertFailsWith<ApiException> { assembler.assemble(listOf(member(Uuid.generateV7().toJavaUuid())), ENGLISH) }

        assertEquals(ProjectError.MEMBER_NOT_FOUND, error.error)
    }

    @Test
    fun `a membership pointing at a role that no longer exists is an error`() {
        val user = userRef().also { users.given(it) }
        val orphaned = ProjectMember.create(projectId = projectId, userId = user.userId, roleId = Uuid.generateV7().toJavaUuid())

        val error = assertFailsWith<ApiException> { assembler.assemble(listOf(orphaned), ENGLISH) }

        assertEquals(ProjectError.ROLE_NOT_FOUND, error.error)
    }
}
