package com.vertyll.veds.iam.application.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ChangeEmailRequest(
    @field:NotBlank(message = "Current password is required")
    val password: String = "",
    @field:NotBlank(message = "New email is required")
    @field:Email(message = "New email should be valid")
    val newEmail: String = "",
)
