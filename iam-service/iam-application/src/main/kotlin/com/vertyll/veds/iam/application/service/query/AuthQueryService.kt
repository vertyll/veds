package com.vertyll.veds.iam.application.service.query

import com.vertyll.veds.iam.application.port.inbound.query.AuthQueryUseCase
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

class AuthQueryService(
    private val userRepository: UserRepository,
) : AuthQueryUseCase {
    override fun getUserPermissions(keycloakId: UUID): List<String> {
        val user =
            userRepository.findByKeycloakId(keycloakId)
                ?: throw ApiException(IamError.USER_NOT_FOUND)
        return user.permissions.map { it.name }
    }
}
