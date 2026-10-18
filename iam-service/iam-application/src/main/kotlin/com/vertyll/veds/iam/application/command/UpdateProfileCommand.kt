package com.vertyll.veds.iam.application.command

import java.util.UUID

data class UpdateProfileCommand(
    val firstName: String,
    val lastName: String,
    val avatarFileId: UUID?,
    val phoneNumber: String?,
    val address: String?,
)
