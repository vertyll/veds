package com.vertyll.veds.iam.domain.model

/**
 * Enumerated identity-and-access roles owned by the iam-service domain.
 *
 * **Single source of truth for role *names*** is the Keycloak realm (see
 * `keycloak/realm-config/realm-export.json`); this enum exists only inside
 * iam-service for type-safe seeding (`RoleInitializer`) and role assignment
 * (`AuthService.register`) — the two places that *administer* roles.
 *
 * Other microservices intentionally do **not** depend on this enum. They
 * only *consume* roles from the JWT `realm_access.roles` claim and check
 * them as opaque strings (e.g. `@PreAuthorize("hasRole('ADMIN')")` or
 * `hasRole("ADMIN")` in `SecurityConfig`). This avoids a shared-kernel
 * coupling: adding a new role does not require recompiling other services.
 */
internal enum class RoleType(
    val value: String,
) {
    USER("USER"),
    ADMIN("ADMIN"),
    ;

    companion object {
        fun fromString(name: String): RoleType? = entries.find { it.value == name }
    }
}

