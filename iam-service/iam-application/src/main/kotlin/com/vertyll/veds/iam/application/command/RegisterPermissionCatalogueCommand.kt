package com.vertyll.veds.iam.application.command

import com.vertyll.veds.iam.domain.model.RoleScope

data class RegisterPermissionCatalogueCommand(
    val module: String,
    val permissions: List<PermissionDeclaration>,
    val stockRoles: List<StockRoleDeclaration> = emptyList(),
) {
    data class PermissionDeclaration(
        val name: String,
        val description: String?,
        val scope: RoleScope,
    )

    data class StockRoleDeclaration(
        val name: String,
        val scope: RoleScope,
        val permissions: Set<String>,
    )
}
