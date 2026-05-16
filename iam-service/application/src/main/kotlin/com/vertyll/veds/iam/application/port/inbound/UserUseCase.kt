package com.vertyll.veds.iam.application.port.inbound

import com.vertyll.veds.iam.application.dto.UpdateProfileRequest
import com.vertyll.veds.iam.application.dto.UserResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserUseCase {
    fun getAllUsers(pageable: Pageable): Page<UserResponse>

    fun getUserById(id: Long): UserResponse

    fun getUserByEmail(email: String): UserResponse

    fun updateProfile(
        id: Long,
        request: UpdateProfileRequest,
        version: Long? = null,
    ): UserResponse
}
