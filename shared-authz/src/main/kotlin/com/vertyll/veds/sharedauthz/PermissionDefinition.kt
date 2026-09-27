package com.vertyll.veds.sharedauthz

/**
 * @property scope where the permission can be held. A permission enforced against
 *           a project membership is meaningless on a platform-wide role, and one
 *           enforced against the platform is meaningless inside a project, so the
 *           module that knows the difference states it and nothing can mix them.
 */
data class PermissionDefinition(
    val name: String,
    val description: String? = null,
    val scope: RoleScope,
) {
    init {
        require(name.matches(NAME_PATTERN)) { "permission name '$name' must be UPPER_SNAKE_CASE" }
    }

    private companion object {
        private val NAME_PATTERN = Regex("^[A-Z][A-Z0-9_]*$")
    }
}
