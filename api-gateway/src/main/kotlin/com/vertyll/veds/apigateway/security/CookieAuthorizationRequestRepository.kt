package com.vertyll.veds.apigateway.security

import com.vertyll.veds.apigateway.session.SessionCipher
import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseCookie
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputFilter
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.GeneralSecurityException
import java.time.Duration
import java.util.Base64

/**
 * Keeps the pending authorization request — state, PKCE verifier, nonce — in an encrypted
 * `SameSite=Lax` cookie rather than in the session. The session cookie is `SameSite=Strict`, so
 * the browser does not send it on Keycloak's redirect back; this cookie it does.
 */
@Component
internal class CookieAuthorizationRequestRepository(
    private val cipher: SessionCipher,
    private val sharedConfig: SharedKeycloakProperties,
) : ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    companion object {
        const val COOKIE = "KEYCLOAK_AUTH_REQUEST"
        private const val SAME_SITE_LAX = "Lax"
        private val LIFETIME: Duration = Duration.ofMinutes(10)
        private val ALLOWED_CLASSES =
            ObjectInputFilter.Config.createFilter(
                "org.springframework.security.oauth2.core.**;java.util.*;java.lang.*;!*",
            )
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    override fun loadAuthorizationRequest(exchange: ServerWebExchange): Mono<OAuth2AuthorizationRequest> {
        val state = exchange.request.queryParams.getFirst(OAuth2ParameterNames.STATE) ?: return Mono.empty()
        return Mono
            .justOrEmpty(
                exchange.request.cookies
                    .getFirst(COOKIE)
                    ?.value,
            ).mapNotNull { open(it) }
            .filter { it.state == state }
    }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest,
        exchange: ServerWebExchange,
    ): Mono<Void> = Mono.fromRunnable { exchange.response.addCookie(cookie(seal(authorizationRequest), LIFETIME)) }

    override fun removeAuthorizationRequest(exchange: ServerWebExchange): Mono<OAuth2AuthorizationRequest> =
        loadAuthorizationRequest(exchange).doOnNext { exchange.response.addCookie(cookie("", Duration.ZERO)) }

    private fun seal(request: OAuth2AuthorizationRequest): String {
        val bytes = ByteArrayOutputStream().also { out -> ObjectOutputStream(out).use { it.writeObject(request) } }.toByteArray()
        return encoder.encodeToString(cipher.encrypt(bytes))
    }

    private fun open(value: String): OAuth2AuthorizationRequest? =
        try {
            ObjectInputStream(ByteArrayInputStream(cipher.decrypt(decoder.decode(value)))).use { input ->
                input.objectInputFilter = ALLOWED_CLASSES
                input.readObject() as? OAuth2AuthorizationRequest
            }
        } catch (e: GeneralSecurityException) {
            log.warn("Ignoring an authorization request cookie that does not decrypt: {}", e.message)
            null
        } catch (e: IllegalArgumentException) {
            log.warn("Ignoring a malformed authorization request cookie: {}", e.message)
            null
        } catch (e: IOException) {
            log.warn("Ignoring an unreadable authorization request cookie: {}", e.message)
            null
        }

    private fun cookie(
        value: String,
        maxAge: Duration,
    ): ResponseCookie =
        ResponseCookie
            .from(COOKIE, value)
            .httpOnly(true)
            .secure(sharedConfig.cookie.secure)
            .sameSite(SAME_SITE_LAX)
            .path(sharedConfig.cookie.path)
            .maxAge(maxAge)
            .build()
}
