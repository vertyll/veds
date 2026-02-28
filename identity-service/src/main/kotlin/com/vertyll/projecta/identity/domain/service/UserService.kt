package com.vertyll.projecta.identity.domain.service

import com.vertyll.projecta.identity.domain.dto.UpdateProfileRequestDto
import com.vertyll.projecta.identity.domain.dto.UserResponseDto
import com.vertyll.projecta.identity.domain.model.entity.User
import com.vertyll.projecta.identity.domain.repository.UserRepository
import com.vertyll.projecta.identity.infrastructure.exception.ApiException
import com.vertyll.projecta.sharedinfrastructure.util.OptimisticLockingValidator
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
    fun getUserById(id: Long): UserResponseDto {
        val user =
            userRepository
                .findById(id)
                .orElseThrow {
                    ApiException(
                        message = USER_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }
        return mapToDto(user)
    }

    @Transactional(readOnly = true)
    fun getUserByEmail(email: String): UserResponseDto {
        val user =
            userRepository
                .findByEmail(email)
                .orElseThrow {
                    ApiException(
                        message = USER_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }
        return mapToDto(user)
    }

    @Transactional
    fun updateProfile(
        id: Long,
        request: UpdateProfileRequestDto,
        version: Long? = null,
    ): UserResponseDto {
        val user =
            userRepository
                .findById(id)
                .orElseThrow {
                    ApiException(
                        message = USER_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }

        OptimisticLockingValidator.validate(user.version, version) {
            ApiException(
                message = USER_VERSION_MISMATCH,
                status = HttpStatus.PRECONDITION_FAILED,
            )
        }

        user.firstName = request.firstName
        user.lastName = request.lastName
        user.profilePicture = request.profilePicture
        user.phoneNumber = request.phoneNumber
        user.address = request.address

        val updatedUser = userRepository.save(user)
        return mapToDto(updatedUser)
    }

    private fun mapToDto(user: User): UserResponseDto =
        UserResponseDto(
            id = user.id!!,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.getEmail(),
            roles = user.roles.map { it.name }.toSet(),
            profilePicture = user.profilePicture,
            phoneNumber = user.phoneNumber,
            address = user.address,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString(),
            version = user.version,
        )
}
