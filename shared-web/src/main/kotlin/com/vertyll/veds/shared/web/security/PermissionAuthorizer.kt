package com.vertyll.veds.shared.web.security

import com.vertyll.veds.sharedauthz.RolePermissionsSource
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Answers `@PreAuthorize("@authz.has('USERS_MANAGE')")`.
 *
 * The token carries the roles a person holds; what a role grants comes from the
 * service's own projection. Nothing is asked of iam-service on the request path,
 * so a decision costs no call and survives its absence, and a permission taken
 * away reaches every service on the next event rather than on token expiry.
 *
 * A service without a [RolePermissionsSource] grants nothing: authorization that
 * cannot read what it needs fails closed.
 */
class PermissionAuthorizer(
    private val source: RolePermissionsSource?,
) {
    /** Whether the current caller holds [permission]. */
    fun has(permission: String): Boolean {
        val roles = currentRoles()
        return !(roles.isEmpty() || source == null) && (source.isUnrestricted(roles) || permission in source.forRoles(roles))
    }

    private fun currentRoles(): Set<String> =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.authorities
            ?.mapNotNull { it.authority?.removePrefix(ROLE_PREFIX)?.takeIf { role -> role.isNotBlank() } }
            ?.toSet()
            .orEmpty()

    private companion object {
        private const val ROLE_PREFIX = "ROLE_"
    }
}
