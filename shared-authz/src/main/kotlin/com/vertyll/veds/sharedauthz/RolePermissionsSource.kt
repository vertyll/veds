package com.vertyll.veds.sharedauthz

/**
 * The role-to-permission projection one service holds for its own module.
 */
interface RolePermissionsSource {
    fun forRoles(roles: Collection<String>): Set<String>

    fun isUnrestricted(roles: Collection<String>): Boolean
}
