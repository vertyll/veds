package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.command.CreateRoleCommand
import com.vertyll.veds.iam.application.command.UpdateRoleCommand
import com.vertyll.veds.iam.application.dto.RoleResponse
import com.vertyll.veds.iam.application.mapper.RoleResponseMapper
import com.vertyll.veds.iam.application.port.inbound.command.RoleCommandUseCase
import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.application.port.outbound.RolePermissionsEventPublisherPort
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.Permission
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.iam.domain.model.RoleType
import com.vertyll.veds.iam.domain.model.VersionGuard
import com.vertyll.veds.iam.domain.repository.PermissionRepository
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.sharederror.ApiException

class RoleCommandService(
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val userRepository: UserRepository,
    private val identityProvider: IdentityProviderPort,
    private val eventPublisher: RolePermissionsEventPublisherPort,
) : RoleCommandUseCase {
    override fun createRole(command: CreateRoleCommand): RoleResponse {
        val name = command.name.trim().uppercase()
        if (roleRepository.existsByName(name)) throw ApiException(IamError.ROLE_ALREADY_EXISTS)

        val saved =
            roleRepository.save(
                Role.create(
                    name = name,
                    description = command.description,
                    permissions = resolve(command.permissions, command.scope),
                    scope = command.scope,
                ),
            )

        if (saved.scope == RoleScope.GLOBAL) identityProvider.createRole(name, command.description)
        eventPublisher.publishChanged(saved)
        return RoleResponseMapper.toResponse(saved)
    }

    override fun updateRole(
        name: String,
        command: UpdateRoleCommand,
        version: Long?,
    ): RoleResponse {
        val role = roleRepository.findByName(name) ?: throw ApiException(IamError.ROLE_NOT_FOUND)

        VersionGuard.requireMatch(role.version, version) {
            ApiException(IamError.VERSION_MISMATCH)
        }

        val updated =
            roleRepository.save(
                role
                    .withDescription(command.description)
                    .withPermissions(resolve(command.permissions, role.scope)),
            )

        eventPublisher.publishChanged(updated)
        return RoleResponseMapper.toResponse(updated)
    }

    override fun deleteRole(name: String) {
        val role = roleRepository.findByName(name) ?: throw ApiException(IamError.ROLE_NOT_FOUND)
        if (RoleType.fromString(role.name) != null) throw ApiException(IamError.ROLE_IS_SYSTEM)
        if (role.unrestricted) throw ApiException(IamError.LAST_UNRESTRICTED_ROLE)
        if (userRepository.countByRole(role.id!!) > 0) throw ApiException(IamError.ROLE_STILL_ASSIGNED)

        roleRepository.delete(role)
        if (role.scope == RoleScope.GLOBAL) identityProvider.deleteRole(role.name)
        eventPublisher.publishRemoved(role.name, role.scope)
    }

    override fun assignRoleToUser(
        userId: Long,
        roleName: String,
        version: Long?,
    ) {
        val user = userRepository.findById(userId) ?: throw ApiException(IamError.USER_NOT_FOUND)

        VersionGuard.requireMatch(user.version, version) {
            ApiException(IamError.VERSION_MISMATCH)
        }

        val role = roleRepository.findByName(roleName) ?: throw ApiException(IamError.ROLE_NOT_FOUND)
        val updated = user.withRole(role)
        userRepository.save(updated)

        updated.keycloakId?.let { identityProvider.assignRole(it.toString(), roleName) }
    }

    override fun removeRoleFromUser(
        userId: Long,
        roleName: String,
        version: Long?,
    ) {
        val user = userRepository.findById(userId) ?: throw ApiException(IamError.USER_NOT_FOUND)

        VersionGuard.requireMatch(user.version, version) {
            ApiException(IamError.VERSION_MISMATCH)
        }

        val role = roleRepository.findByName(roleName) ?: throw ApiException(IamError.ROLE_NOT_FOUND)
        requireSomebodyKeepsEveryPermission(role)

        val updated = user.withoutRole(role.id!!)
        userRepository.save(updated)

        updated.keycloakId?.let { identityProvider.removeRole(it.toString(), roleName) }
    }

    private fun requireSomebodyKeepsEveryPermission(role: Role) {
        if (!role.unrestricted) return

        val stillUnrestricted =
            roleRepository
                .findAll()
                .filter { it.unrestricted && it.id != role.id }
                .any { userRepository.countByRole(it.id!!) > 0 }
        if (stillUnrestricted) return

        if (userRepository.countByRole(role.id!!) <= 1) throw ApiException(IamError.LAST_UNRESTRICTED_ROLE)
    }

    private fun resolve(
        names: Set<String>,
        scope: RoleScope,
    ): Set<Permission> {
        if (names.isEmpty()) return emptySet()

        val found = permissionRepository.findAllByNames(names)
        val missing = names - found.mapTo(mutableSetOf()) { it.name }
        if (missing.isNotEmpty()) throw ApiException(IamError.PERMISSION_NOT_FOUND)

        if (found.any { it.scope != scope }) throw ApiException(IamError.PERMISSION_OUT_OF_SCOPE)

        return found.toSet()
    }
}
