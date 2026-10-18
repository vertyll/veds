package com.vertyll.veds.iam.application.mapper

import com.vertyll.veds.iam.application.dto.RoleResponse
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleType

internal object RoleResponseMapper {
    fun toResponse(role: Role): RoleResponse =
        RoleResponse(
            id = role.id!!,
            name = role.name,
            description = role.description,
            permissions = role.permissions.map { it.name }.sorted(),
            unrestricted = role.unrestricted,
            scope = role.scope.name,
            system = RoleType.fromString(role.name) != null,
            version = role.version,
        )
}
