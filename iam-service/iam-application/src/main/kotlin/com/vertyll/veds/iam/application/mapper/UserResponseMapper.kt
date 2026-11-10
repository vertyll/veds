package com.vertyll.veds.iam.application.mapper

import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.User

internal object UserResponseMapper {
    fun toResponse(
        user: User,
        heldRoles: Collection<Role>,
    ): UserResponse =
        UserResponse(
            id = user.storedId,
            keycloakId = user.keycloakId?.toString(),
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            roles = user.roles,
            permissions = user.permissionsFrom(heldRoles),
            avatarFileId = user.avatarFileId,
            phoneNumber = user.phoneNumber,
            address = user.address,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString(),
            version = user.version,
        )
}
