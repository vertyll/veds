@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.iam.application

import com.vertyll.veds.iam.domain.model.Permission
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.iam.domain.model.User
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal fun user(
    id: Long = 1L,
    email: String = "ada@example.com",
    keycloakId: UUID? = Uuid.generateV7().toJavaUuid(),
    roles: Set<Role> = emptySet(),
) = User(id = id, keycloakId = keycloakId, email = email, firstName = "Ada", lastName = "Lovelace", roles = roles)

internal fun role(
    id: Long = 1L,
    name: String = "USER",
    permissions: Set<Permission> = emptySet(),
    unrestricted: Boolean = false,
) = Role(id = id, name = name, permissions = permissions, unrestricted = unrestricted)

internal fun permission(
    id: Long = 1L,
    name: String = "TASKS_VIEW",
    module: String = "task",
    scope: RoleScope = RoleScope.PROJECT,
    description: String? = null,
) = Permission(id = id, name = name, module = module, scope = scope, description = description)
