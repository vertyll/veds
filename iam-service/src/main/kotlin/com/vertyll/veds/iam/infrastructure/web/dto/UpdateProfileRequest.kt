package com.vertyll.veds.iam.infrastructure.web.dto

import jakarta.validation.constraints.NotBlank

data class UpdateProfileRequest(
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    val lastName: String,
    val profilePicture: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
)
