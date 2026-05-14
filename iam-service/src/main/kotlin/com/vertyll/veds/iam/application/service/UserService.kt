package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.infrastructure.exception.ApiException
import com.vertyll.veds.iam.infrastructure.web.dto.UpdateProfileRequest
import com.vertyll.veds.iam.infrastructure.web.dto.UserResponse
import com.vertyll.veds.sharedinfrastructure.util.OptimisticLockingValidator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    private companion object {
        private const val USER_NOT_FOUND = "User not found"
        private const val USER_VERSION_MISMATCH = "Precondition Failed: User version mismatch"
    }

    @Transactional(readOnly = true)
    fun getAllUsers(pageable: Pageable): Page<UserResponse> = userRepository.findAll(pageable).map { mapToDto(it) }

    @Transactional(readOnly = true)
    fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id) ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)
        return mapToDto(user)
    }

    @Transactional(readOnly = true)
    fun getUserByEmail(email: String): UserResponse {
        val user = userRepository.findByEmail(email) ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)
        return mapToDto(user)
    }

    @Transactional
    fun updateProfile(
        id: Long,
        request: UpdateProfileRequest,
        version: Long? = null,
    ): UserResponse {
        val user = userRepository.findById(id) ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        OptimisticLockingValidator.validate(user.version, version) {
            ApiException(USER_VERSION_MISMATCH, HttpStatus.PRECONDITION_FAILED)
        }

        val updated =
            user.withProfile(
                firstName = request.firstName,
                lastName = request.lastName,
                profilePicture = request.profilePicture,
                phoneNumber = request.phoneNumber,
                address = request.address,
            )
        return mapToDto(userRepository.save(updated))
    }

    private fun mapToDto(user: User): UserResponse =
        UserResponse(
            id = user.id!!,
            keycloakId = user.keycloakId?.toString(),
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            roles = user.roles.map { it.name }.toSet(),
            permissions = user.permissions.map { it.name }.toSet(),
            profilePicture = user.profilePicture,
            phoneNumber = user.phoneNumber,
            address = user.address,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString(),
            version = user.version,
        )
}
