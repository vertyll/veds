package com.vertyll.veds.sharedinfrastructure.utils

import org.springframework.security.oauth2.jwt.Jwt

/**
 * JWT claim helpers for Keycloak tokens, used by both the servlet and
 * reactive `KeycloakJwtAuthenticationConverter` to keep claim extraction
 * logic in one place.
 */
object KeycloakJwtUtils {
    /**
     * Extracts the list of role strings located at [rolesClaimPath] within
     * the JWT [jwt].
     *
     * The path is dot-separated and walks nested JSON objects, which covers
     * both flat layouts (`"roles"`) and Keycloak's default nested layout
     * (`"realm_access.roles"`). Returns an empty list when any segment is
     * missing or when the leaf is not a JSON array of strings — this keeps
     * the converter agnostic of the role vocabulary defined in Keycloak.
     */
    fun extractRoles(
        jwt: Jwt,
        rolesClaimPath: String,
    ): List<String> {
        val pathParts = rolesClaimPath.split(".")

        var current: Any? = jwt.claims[pathParts[0]]
        for (part in pathParts.drop(1)) {
            current = (current as? Map<*, *>)?.get(part)
        }

        return (current as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    }
}
