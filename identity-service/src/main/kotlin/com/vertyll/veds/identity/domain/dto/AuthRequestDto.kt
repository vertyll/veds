package com.vertyll.veds.identity.domain.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class AuthRequestDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String = "",
    @field:NotBlank(message = "Password is required")
    val password: String = "",
    val deviceInfo: String? = null,
    val userAgent: String? = null,
)
