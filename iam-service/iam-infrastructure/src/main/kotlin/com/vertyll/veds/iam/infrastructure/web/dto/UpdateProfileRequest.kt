package com.vertyll.veds.iam.infrastructure.web.dto

import com.vertyll.veds.iam.application.command.UpdateProfileCommand
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class UpdateProfileRequest(
    @field:NotBlank(message = "validation.iam.first_name_required")
    val firstName: String,
    @field:NotBlank(message = "validation.iam.last_name_required")
    val lastName: String,
    val avatarFileId: UUID? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
) {
    fun toCommand(): UpdateProfileCommand =
        UpdateProfileCommand(
            firstName = firstName,
            lastName = lastName,
            avatarFileId = avatarFileId,
            phoneNumber = phoneNumber,
            address = address,
        )
}
