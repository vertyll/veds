package com.vertyll.veds.iam.infrastructure.web.dto

import jakarta.validation.constraints.Size

data class UpdateRoleRequest(
    @field:Size(max = 255)
    val description: String? = null,
    val permissions: Set<String> = emptySet(),
)
