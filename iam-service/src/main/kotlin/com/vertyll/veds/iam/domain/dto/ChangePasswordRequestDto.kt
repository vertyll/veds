package com.vertyll.veds.iam.domain.dto

import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequestDto(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String = "",
)
