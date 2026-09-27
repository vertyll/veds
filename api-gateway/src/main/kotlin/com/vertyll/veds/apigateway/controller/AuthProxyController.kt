package com.vertyll.veds.apigateway.controller

import com.vertyll.veds.apigateway.config.GatewayOAuthProperties
import com.vertyll.veds.apigateway.security.AuthTransactionCookies
import com.vertyll.veds.apigateway.security.Pkce
import com.vertyll.veds.apigateway.session.KeycloakTokenClient
import com.vertyll.veds.apigateway.session.SessionCookies
import com.vertyll.veds.apigateway.session.SessionStore
import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import com.vertyll.veds.shared.web.security.PublicEndpoint
import com.vertyll.veds.shared.web.security.ScopedToCaller
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI

@RestController
@RequestMapping("/auth")
internal class AuthProxyController(
    private val sharedConfig: SharedKeycloakProperties,
    private val oauth: GatewayOAuthProperties,
    private val authTransactionCookies: AuthTransactionCookies,
    private val sessionCookies: SessionCookies,
    private val sessionStore: SessionStore,
    private val keycloakTokenClient: KeycloakTokenClient,
) {
    private companion object {
        private val log = LoggerFactory.getLogger(AuthProxyController::class.java)

        private const val ERROR_PARAM = "error"
        private const val ERR_STATE_MISMATCH = "state_mismatch"
        private const val ERR_MISSING_VERIFIER = "missing_code_verifier"
        private const val ERR_CODE_EXCHANGE_FAILED = "code_exchange_failed"

        private const val KC_ACTION_PARAM = "kc_action"

        private val ALLOWED_KC_ACTIONS = setOf("CONFIGURE_TOTP", "UPDATE_PASSWORD", "delete_credential")

        private const val SCOPE = "openid profile email"
    }

    data class SessionResponse(
        val userId: String,
        val email: String,
        val roles: List<String>,
    )

    @Suppress("kotlin:S6508")
    @GetMapping("/authorize")
    @PublicEndpoint("the start of sign-in: there is no token yet, and this only builds the Keycloak redirect")
    fun authorize(
        exchange: ServerWebExchange,
        @RequestParam(name = "kc_action", required = false) kcAction: String?,
    ): ResponseEntity<Void> {
        val state = Pkce.newState()
        val codeVerifier = Pkce.newCodeVerifier()

        authTransactionCookies.issue(exchange, state = state, codeVerifier = codeVerifier)

        val authorizationUri =
            UriComponentsBuilder
                .fromUriString(keycloakAuthorizationUrl())
                .queryParam("client_id", sharedConfig.gatewayClientId)
                .queryParam("redirect_uri", oauth.redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .queryParam("code_challenge", Pkce.challengeOf(codeVerifier))
                .queryParam("code_challenge_method", Pkce.CHALLENGE_METHOD)
                .apply { allowedAction(kcAction)?.let { queryParam(KC_ACTION_PARAM, it) } }
                .encode()
                .build()
                .toUriString()

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authorizationUri)).build()
    }

    private fun allowedAction(kcAction: String?): String? = kcAction?.takeIf { it in ALLOWED_KC_ACTIONS }

    @Suppress("kotlin:S6508")
    @GetMapping("/callback")
    @PublicEndpoint("Keycloak redirects the browser back here with a code; the code, not a token, is the credential")
    fun callback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(name = ERROR_PARAM, required = false) error: String?,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<Void>> {
        val expectedState = authTransactionCookies.read(exchange, AuthTransactionCookies.STATE_COOKIE)
        val codeVerifier = authTransactionCookies.read(exchange, AuthTransactionCookies.VERIFIER_COOKIE)
        authTransactionCookies.clear(exchange)

        if (error != null) {
            log.debug("Keycloak returned an authorization error: {}", error)
            return Mono.just(redirectToApp(error))
        }

        val stateMatches = state != null && expectedState != null && state == expectedState
        if (code == null || !stateMatches) {
            log.warn("Rejecting callback: state does not match the value issued to this browser")
            return Mono.just(redirectToApp(ERR_STATE_MISMATCH))
        }

        if (codeVerifier == null) {
            log.warn("Rejecting callback: no code_verifier cookie present")
            return Mono.just(redirectToApp(ERR_MISSING_VERIFIER))
        }

        return keycloakTokenClient
            .exchangeAuthorizationCode(code, codeVerifier)
            .flatMap { session -> sessionStore.create(session) }
            .map { sessionId ->
                sessionCookies.issue(exchange, sessionId)
                redirectToApp(null)
            }.onErrorResume { ex ->
                log.warn("Authorization code exchange failed: {}", ex.message)
                Mono.just(redirectToApp(ERR_CODE_EXCHANGE_FAILED))
            }
    }

    @Suppress("kotlin:S6508")
    @GetMapping("/session")
    @ScopedToCaller("reads the session cookie the caller already holds, and answers 204 when there is none")
    fun session(exchange: ServerWebExchange): Mono<ResponseEntity<SessionResponse>> {
        val sessionId = sessionCookies.read(exchange) ?: return Mono.just(noSession())

        return sessionStore
            .find(sessionId)
            .map { session ->
                ResponseEntity.ok(
                    SessionResponse(
                        userId = session.subject,
                        email = session.email,
                        roles = session.roles,
                    ),
                )
            }.switchIfEmpty(Mono.fromCallable { noSession() })
    }

    @Suppress("kotlin:S6508")
    @PostMapping("/logout")
    @ScopedToCaller("invalidates the refresh token in the caller's own cookie")
    fun logout(exchange: ServerWebExchange): Mono<ResponseEntity<Void>> {
        val sessionId = sessionCookies.read(exchange)
        sessionCookies.clear(exchange)

        if (sessionId == null) return Mono.just(loggedOut())

        return sessionStore
            .find(sessionId)
            .flatMap { session -> keycloakTokenClient.revoke(session.refreshToken) }
            .then(sessionStore.delete(sessionId))
            .thenReturn(loggedOut())
            .onErrorResume { ex ->
                log.warn("Keycloak revocation failed, local session destroyed anyway: {}", ex.message)
                sessionStore.delete(sessionId).thenReturn(loggedOut())
            }
    }

    @Suppress("kotlin:S6508")
    private fun noSession(): ResponseEntity<SessionResponse> = ResponseEntity.noContent().build()

    @Suppress("kotlin:S6508")
    private fun loggedOut(): ResponseEntity<Void> = ResponseEntity.noContent().build()

    @Suppress("kotlin:S6508")
    private fun redirectToApp(error: String?): ResponseEntity<Void> {
        val target =
            UriComponentsBuilder
                .fromUriString(oauth.postLoginRedirectUri)
                .apply { if (error != null) queryParam(ERROR_PARAM, error) }
                .encode()
                .build()
                .toUriString()
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build()
    }

    private fun keycloakAuthorizationUrl(): String = "${sharedConfig.serverUrl}/realms/${sharedConfig.realm}/protocol/openid-connect/auth"
}
