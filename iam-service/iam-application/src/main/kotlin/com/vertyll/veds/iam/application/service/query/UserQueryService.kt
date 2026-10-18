package com.vertyll.veds.iam.application.service.query

import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.application.mapper.UserResponseMapper
import com.vertyll.veds.iam.application.port.inbound.query.UserQueryUseCase
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

class UserQueryService(
    private val userRepository: UserRepository,
) : UserQueryUseCase {
    override fun getAllUsers(pageRequest: PageRequest): PageResult<UserResponse> =
        userRepository.findAll(pageRequest).map(UserResponseMapper::toResponse)

    override fun searchUsers(
        term: String,
        pageRequest: PageRequest,
    ): PageResult<UserResponse> =
        if (term.isBlank()) {
            getAllUsers(pageRequest)
        } else {
            userRepository.search(term.trim(), pageRequest).map(UserResponseMapper::toResponse)
        }

    override fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id) ?: throw ApiException(IamError.USER_NOT_FOUND)
        return UserResponseMapper.toResponse(user)
    }

    override fun getUserByEmail(email: String): UserResponse {
        val user = userRepository.findByEmail(email) ?: throw ApiException(IamError.USER_NOT_FOUND)
        return UserResponseMapper.toResponse(user)
    }

    override fun getUserByKeycloakId(keycloakId: UUID): UserResponse {
        val user = userRepository.findByKeycloakId(keycloakId) ?: throw ApiException(IamError.USER_NOT_FOUND)
        return UserResponseMapper.toResponse(user)
    }
}
