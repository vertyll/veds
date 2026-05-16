package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.dto.UpdateProfileRequest
import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.port.inbound.UserUseCase
import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.sharedinfrastructure.utils.OptimisticLockingValidatorUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.vertyll.veds.iam.domain.model.PageRequest as DomainPageRequest

@Service
internal class UserService(
    private val userRepository: UserRepository,
) : UserUseCase {
    private companion object {
        private const val USER_NOT_FOUND = "User not found"
        private const val USER_VERSION_MISMATCH = "Precondition Failed: User version mismatch"
    }

    @Transactional(readOnly = true)
    override fun getAllUsers(pageable: Pageable): Page<UserResponse> {
        val domainResult =
            userRepository.findAll(
                DomainPageRequest(
                    page = pageable.pageNumber,
                    size = pageable.pageSize,
                ),
            )
        return PageImpl(
            domainResult.content.map(::mapToDto),
            pageable,
            domainResult.totalElements,
        )
    }

    @Transactional(readOnly = true)
    override fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id) ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)
        return mapToDto(user)
    }

    @Transactional(readOnly = true)
    override fun getUserByEmail(email: String): UserResponse {
        val user = userRepository.findByEmail(email) ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)
        return mapToDto(user)
    }

    @Transactional
    override fun updateProfile(
        id: Long,
        request: UpdateProfileRequest,
        version: Long?,
    ): UserResponse {
        val user = userRepository.findById(id) ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        OptimisticLockingValidatorUtils.validate(user.version, version) {
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
