package com.vertyll.veds.sharedinfrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "veds.shared")
data class SharedConfigProperties(
    val security: SecurityProperties,
) {
    data class SecurityProperties(
        val jwt: JwtProperties,
    ) {
        data class JwtProperties(
            val secretKey: String,
            val accessTokenExpiration: Long,
            val refreshTokenExpiration: Long,
            val refreshTokenCookieName: String,
            val authHeaderName: String,
        )
    }
}
