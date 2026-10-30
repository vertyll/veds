package com.vertyll.veds.sharedauthz

/**
 * The role-to-permission projection one service holds for its own module.
 */
interface RolePermissionsSource {
    /** The permissions that [roles] grant together. */
    fun forRoles(roles: Collection<String>): Set<String>

    /** Whether any of [roles] is allowed everything. */
    fun isUnrestricted(roles: Collection<String>): Boolean
}
