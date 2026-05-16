package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.dto.RoleResponse
import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.port.inbound.RoleUseCase
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.sharedinfrastructure.utils.OptimisticLockingValidatorUtils
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class RoleService(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val identityProvider: IdentityProviderPort,
) : RoleUseCase {
    private companion object {
        private const val ROLE_NOT_FOUND = "Role not found"
        private const val USER_NOT_FOUND = "User not found"
        private const val PRECONDITION_FAILED = "Precondition Failed: Version mismatch"
    }

    @Transactional(readOnly = true)
    override fun getRoleById(id: Long): RoleResponse {
        val role = roleRepository.findById(id) ?: throw ApiException(ROLE_NOT_FOUND, HttpStatus.NOT_FOUND)
        return mapToDto(role)
    }

    @Transactional(readOnly = true)
    override fun getRoleByName(name: String): RoleResponse {
        val role = roleRepository.findByName(name) ?: throw ApiException(ROLE_NOT_FOUND, HttpStatus.NOT_FOUND)
        return mapToDto(role)
    }

    @Transactional(readOnly = true)
    override fun getAllRoles(): List<RoleResponse> = roleRepository.findAll().map { mapToDto(it) }

    @Transactional(readOnly = true)
    override fun getRolesForUser(userId: Long): List<RoleResponse> {
        val user = userRepository.findById(userId) ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)
        return user.roles.map { mapToDto(it) }
    }

    @Transactional
    override fun assignRoleToUser(
        userId: Long,
        roleName: String,
        version: Long?,
    ) {
        val user = userRepository.findById(userId) ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        OptimisticLockingValidatorUtils.validate(user.version, version) {
            ApiException(PRECONDITION_FAILED, HttpStatus.PRECONDITION_FAILED)
        }

        val role = roleRepository.findByName(roleName) ?: throw ApiException(ROLE_NOT_FOUND, HttpStatus.NOT_FOUND)
        val updated = user.withRole(role)
        userRepository.save(updated)

        updated.keycloakId?.let { identityProvider.assignRole(it.toString(), roleName) }
    }

    @Transactional
    override fun removeRoleFromUser(
        userId: Long,
        roleName: String,
        version: Long?,
    ) {
        val user = userRepository.findById(userId) ?: throw ApiException(USER_NOT_FOUND, HttpStatus.NOT_FOUND)

        OptimisticLockingValidatorUtils.validate(user.version, version) {
            ApiException(PRECONDITION_FAILED, HttpStatus.PRECONDITION_FAILED)
        }

        val role = roleRepository.findByName(roleName) ?: throw ApiException(ROLE_NOT_FOUND, HttpStatus.NOT_FOUND)
        val updated = user.withoutRole(role.id!!)
        userRepository.save(updated)

        updated.keycloakId?.let { identityProvider.removeRole(it.toString(), roleName) }
    }

    private fun mapToDto(role: Role): RoleResponse =
        RoleResponse(
            id = role.id!!,
            name = role.name,
            description = role.description,
            version = role.version,
        )
}
