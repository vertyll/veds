package com.vertyll.veds.iam.application.port.inbound.command

import com.vertyll.veds.iam.application.command.UpdateProfileCommand
import com.vertyll.veds.iam.application.dto.UserResponse

@Suppress("kotlin:S6517")
interface UserCommandUseCase {
    fun updateProfile(
        id: Long,
        request: UpdateProfileCommand,
        version: Long? = null,
    ): UserResponse
}
