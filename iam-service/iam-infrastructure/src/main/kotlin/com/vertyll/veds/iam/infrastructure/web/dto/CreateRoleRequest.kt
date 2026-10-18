package com.vertyll.veds.iam.infrastructure.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateRoleRequest(
    @field:NotBlank
    @field:Size(max = 64)
    @field:Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$")
    val name: String,
    @field:Size(max = 255)
    val description: String? = null,
    val permissions: Set<String> = emptySet(),
    @field:Pattern(regexp = "^(GLOBAL|PROJECT)$")
    val scope: String = "GLOBAL",
)
