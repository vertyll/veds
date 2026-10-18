package com.vertyll.veds.iam.application.mapper

import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.domain.model.User

internal object UserResponseMapper {
    fun toResponse(user: User): UserResponse =
        UserResponse(
            id = user.id!!,
            keycloakId = user.keycloakId?.toString(),
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            roles = user.roles.map { it.name }.toSet(),
            permissions = user.permissions.map { it.name }.toSet(),
            avatarFileId = user.avatarFileId,
            phoneNumber = user.phoneNumber,
            address = user.address,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString(),
            version = user.version,
        )
}
