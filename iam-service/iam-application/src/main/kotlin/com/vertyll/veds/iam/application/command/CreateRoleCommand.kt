package com.vertyll.veds.iam.application.command

import com.vertyll.veds.iam.domain.model.RoleScope

data class CreateRoleCommand(
    val name: String,
    val description: String?,
    val permissions: Set<String>,
    val scope: RoleScope = RoleScope.GLOBAL,
)
