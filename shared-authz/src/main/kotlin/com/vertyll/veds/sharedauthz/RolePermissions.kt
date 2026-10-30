package com.vertyll.veds.sharedauthz

/**
 * What one role grants inside a single module.
 *
 * Every service keeps its own projection of these, fed by events, and decides
 * locally. A service therefore authorizes correctly while iam-service is down —
 * it answers from the last set it was told about rather than asking.
 */
data class RolePermissions(
    /** The name of the role. */
    val role: String,
    /** The module these permissions belong to. */
    val module: String,
    /** The permissions the role grants in [module]. */
    val permissions: Set<String>,
    /**
     * A role that holds everything the module has, now and after it grows. The
     * administrator is expressed this way rather than as a list, so a module
     * added tomorrow is covered on the day it ships instead of when somebody
     * remembers to tick its boxes.
     */
    val unrestricted: Boolean = false,
) {
    /** Whether the role grants [permission]. */
    fun grants(permission: String): Boolean = unrestricted || permission in permissions
}
