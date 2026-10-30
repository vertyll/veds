package com.vertyll.veds.sharedauthz

/**
 * A role the module ships with, and what it starts out granting.
 *
 * The grant applies only where iam-service has no such role yet. Once the role
 * exists an administrator owns it, so redeploying a module never silently
 * restores permissions somebody deliberately took away.
 */
data class StockRole(
    /** The name of the role. */
    val name: String,
    /** Where the role applies. */
    val scope: RoleScope,
    /** The permissions the role grants. */
    val permissions: Set<String>,
) {
    init {
        require(name.matches(ROLE_NAME_PATTERN)) { "role name '$name' must be UPPER_SNAKE_CASE" }
    }

    private companion object {
        private val ROLE_NAME_PATTERN = Regex("^[A-Z][A-Z0-9_]*$")
    }
}
