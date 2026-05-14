package com.vertyll.veds.iam.infrastructure.web.dto

import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String = "",
)
