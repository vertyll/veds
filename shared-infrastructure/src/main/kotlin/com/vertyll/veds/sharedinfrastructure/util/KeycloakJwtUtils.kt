package com.vertyll.veds.sharedinfrastructure.util

import org.springframework.security.oauth2.jwt.Jwt

object KeycloakJwtUtils {
    // Supports both flat ("roles") and nested ("realm_access.roles") claim paths
    fun extractRoles(
        jwt: Jwt,
        rolesClaimPath: String,
    ): List<String> {
        val pathParts = rolesClaimPath.split(".")

        var current: Any? = jwt.claims[pathParts[0]]
        for (part in pathParts.drop(1)) {
            current = (current as? Map<*, *>)?.get(part)
        }

        @Suppress("UNCHECKED_CAST")
        return (current as? List<String>) ?: emptyList()
    }
}
