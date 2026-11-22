package com.vertyll.veds.apigateway.security

import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository

/**
 * The cookie holding the CSRF token. It is `HttpOnly`, as FastDo, on another origin, learns the token from the [HEADER]
 * of `GET /auth/session` instead; the `__Host-` prefix keeps a sibling subdomain from planting a token of its own.
 */
internal object CsrfTokenCookie {
    const val HEADER = "X-XSRF-TOKEN"
    private const val NAME = "XSRF-TOKEN"
    private const val HOST_PREFIX = "__Host-"
    private const val PATH = "/"
    private const val SAME_SITE_STRICT = "Strict"

    fun repository(secure: Boolean): ServerCsrfTokenRepository =
        CookieServerCsrfTokenRepository().apply {
            setCookieName(if (secure) HOST_PREFIX + NAME else NAME)
            setHeaderName(HEADER)
            setCookiePath(PATH)
            setCookieCustomizer { cookie -> cookie.secure(secure).sameSite(SAME_SITE_STRICT) }
        }
}
