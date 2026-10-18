package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.command.UpdateProfileCommand
import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.application.mapper.UserResponseMapper
import com.vertyll.veds.iam.application.port.inbound.command.UserCommandUseCase
import com.vertyll.veds.iam.application.port.outbound.AuthEventPublisherPort
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.VersionGuard
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.sharederror.ApiException

class UserCommandService(
    private val userRepository: UserRepository,
    private val authEventPublisher: AuthEventPublisherPort,
) : UserCommandUseCase {
    override fun updateProfile(
        id: Long,
        request: UpdateProfileCommand,
        version: Long?,
    ): UserResponse {
        val user = userRepository.findById(id) ?: throw ApiException(IamError.USER_NOT_FOUND)

        VersionGuard.requireMatch(user.version, version) {
            ApiException(IamError.USER_VERSION_MISMATCH)
        }

        val updated =
            user.withProfile(
                firstName = request.firstName,
                lastName = request.lastName,
                avatarFileId = request.avatarFileId,
                phoneNumber = request.phoneNumber,
                address = request.address,
            )
        val saved = userRepository.save(updated)

        saved.keycloakId?.let { keycloakId ->
            authEventPublisher.publishUserProfileUpdated(
                userId = keycloakId,
                email = saved.email,
                firstName = saved.firstName,
                lastName = saved.lastName,
                avatarFileId = saved.avatarFileId,
            )
        }

        return UserResponseMapper.toResponse(saved)
    }
}
