package com.vertyll.veds.iam.application.port.inbound.query

import com.vertyll.veds.iam.application.dto.UserResponse
import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult
import java.util.UUID

interface UserQueryUseCase {
    fun getAllUsers(pageRequest: PageRequest): PageResult<UserResponse>

    fun searchUsers(
        term: String,
        pageRequest: PageRequest,
    ): PageResult<UserResponse>

    fun getUserById(id: Long): UserResponse

    fun getUserByEmail(email: String): UserResponse

    fun getUserByKeycloakId(keycloakId: UUID): UserResponse
}
