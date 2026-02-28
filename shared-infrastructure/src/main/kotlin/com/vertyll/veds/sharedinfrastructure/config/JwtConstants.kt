package com.vertyll.veds.sharedinfrastructure.config

/**
 * Constants used for JWT (JSON Web Token) handling in the application.
 */
object JwtConstants {
    // JWT field names and claim keys
    const val CLAIM_ROLES = "roles"
    const val CLAIM_TOKEN_ID = "tokenId"
    const val CLAIM_TYPE = "type"

    // Token types
    const val TOKEN_TYPE_ACCESS = "access"
    const val TOKEN_TYPE_REFRESH = "refresh"

    // Header constants
    const val BEARER_PREFIX = "Bearer "
}
