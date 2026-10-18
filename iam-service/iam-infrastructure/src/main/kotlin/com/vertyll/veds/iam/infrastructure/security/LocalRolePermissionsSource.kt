package com.vertyll.veds.iam.infrastructure.security

import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.sharedauthz.RolePermissionsSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class LocalRolePermissionsSource(
    private val roleRepository: RoleRepository,
) : RolePermissionsSource {
    @Transactional(readOnly = true)
    override fun forRoles(roles: Collection<String>): Set<String> =
        roleRepository
            .findAllByNames(roles)
            .flatMapTo(mutableSetOf()) { role -> role.permissions.map { it.name } }

    @Transactional(readOnly = true)
    override fun isUnrestricted(roles: Collection<String>): Boolean = roleRepository.findAllByNames(roles).any { it.unrestricted }
}
