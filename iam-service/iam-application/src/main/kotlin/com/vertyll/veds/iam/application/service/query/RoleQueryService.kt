package com.vertyll.veds.iam.application.service.query

import com.vertyll.veds.iam.application.dto.RoleResponse
import com.vertyll.veds.iam.application.mapper.RoleResponseMapper
import com.vertyll.veds.iam.application.port.inbound.query.RoleQueryUseCase
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.sharederror.ApiException

class RoleQueryService(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
) : RoleQueryUseCase {
    override fun getRoleById(id: Long): RoleResponse {
        val role = roleRepository.findById(id) ?: throw ApiException(IamError.ROLE_NOT_FOUND)
        return RoleResponseMapper.toResponse(role)
    }

    override fun getRoleByName(name: String): RoleResponse {
        val role = roleRepository.findByName(name) ?: throw ApiException(IamError.ROLE_NOT_FOUND)
        return RoleResponseMapper.toResponse(role)
    }

    override fun getRolesInScope(scope: RoleScope): List<RoleResponse> =
        roleRepository.findAll().filter { it.scope == scope }.map(RoleResponseMapper::toResponse)

    override fun getAllRoles(): List<RoleResponse> = roleRepository.findAll().map(RoleResponseMapper::toResponse)

    override fun getRolesForUser(userId: Long): List<RoleResponse> {
        val user = userRepository.findById(userId) ?: throw ApiException(IamError.USER_NOT_FOUND)
        return user.roles.map(RoleResponseMapper::toResponse)
    }
}
