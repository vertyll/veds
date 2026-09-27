package com.vertyll.veds.iam.infrastructure.web.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterPermissionCatalogueRequest(
    @field:NotBlank
    @field:Size(max = 64)
    @field:Pattern(regexp = "^[a-z][a-z0-9-]*$")
    val module: String,
    @field:NotEmpty
    val permissions: List<@Valid PermissionDeclarationRequest>,
    val stockRoles: List<@Valid StockRoleRequest> = emptyList(),
) {
    data class PermissionDeclarationRequest(
        @field:NotBlank
        @field:Size(max = 128)
        @field:Pattern(regexp = "^[A-Z][A-Z0-9_]*$")
        val name: String,
        @field:Size(max = 255)
        val description: String? = null,
        @field:NotBlank
        @field:Pattern(regexp = "^(GLOBAL|PROJECT)$")
        val scope: String = "PROJECT",
    )

    data class StockRoleRequest(
        @field:NotBlank
        @field:Size(max = 64)
        @field:Pattern(regexp = "^[A-Z][A-Z0-9_]*$")
        val name: String,
        @field:NotBlank
        @field:Pattern(regexp = "^(GLOBAL|PROJECT)$")
        val scope: String,
        val permissions: Set<String> = emptySet(),
    )
}
