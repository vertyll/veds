package com.vertyll.veds.iam.application.service.query

import com.vertyll.veds.iam.application.InMemoryRoleRepository
import com.vertyll.veds.iam.application.InMemoryUserRepository
import com.vertyll.veds.iam.application.role
import com.vertyll.veds.iam.application.user
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class AuthQueryServiceTest {
    private val users = InMemoryUserRepository()
    private val roles = InMemoryRoleRepository()

    private val service = AuthQueryService(users, roles)

    @Test
    fun `a user's permissions are what the roles it holds grant`() {
        roles.given(
            role(id = 1L, name = "LEAD", permissions = setOf("TASKS_VIEW", "TASKS_MANAGE")),
            role(id = 2L, name = "AUDITOR", permissions = setOf("AUDIT_VIEW")),
        )
        val holder = user(roles = setOf("LEAD")).also { users.given(it) }

        assertEquals(setOf("TASKS_VIEW", "TASKS_MANAGE"), service.getUserPermissions(holder.keycloakId!!).toSet())
    }
}
