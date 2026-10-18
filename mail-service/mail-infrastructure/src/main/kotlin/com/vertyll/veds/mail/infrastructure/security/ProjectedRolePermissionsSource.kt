package com.vertyll.veds.mail.infrastructure.security

import com.vertyll.veds.mail.infrastructure.persistence.repository.RolePermissionsJpaRepository
import com.vertyll.veds.sharedauthz.RolePermissionsSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class ProjectedRolePermissionsSource(
    private val repository: RolePermissionsJpaRepository,
) : RolePermissionsSource {
    @Transactional(readOnly = true)
    override fun forRoles(roles: Collection<String>): Set<String> =
        repository.findAllById(roles).flatMapTo(mutableSetOf()) { it.permissions }

    @Transactional(readOnly = true)
    override fun isUnrestricted(roles: Collection<String>): Boolean = repository.findAllById(roles).any { it.unrestricted }
}
