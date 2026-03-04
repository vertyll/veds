package com.vertyll.veds.apigateway.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.vertyll.veds.sharedinfrastructure.config.SharedConfigProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * BFF (Backend-For-Frontend) controller that proxies authentication requests to Keycloak.
 *
 * Handles:
 * - POST /auth/token — login (username + password → access_token + refresh_token in cookie)
 * - POST /auth/refresh-token — refresh (reads refresh_token from cookie)
 * - POST /auth/logout — logout (invalidates refresh_token, clears cookie)
 */
@RestController
@RequestMapping("/auth")
class AuthProxyController(
    private val sharedConfig: SharedConfigProperties,
) {
    private val log = LoggerFactory.getLogger(AuthProxyController::class.java)

    private val webClient: WebClient by lazy {
        WebClient
            .builder()
            .baseUrl(keycloakTokenUrl())
            .build()
    }

    private val logoutClient: WebClient by lazy {
        WebClient
            .builder()
            .baseUrl(keycloakLogoutUrl())
            .build()
    }

    data class LoginRequest(
        val email: String,
        val password: String,
    )

    data class TokenResponse(
        val accessToken: String,
        val expiresIn: Long,
        val tokenType: String,
    )

    /**
     * Login: exchanges user credentials for Keycloak tokens.
     * Returns access_token in body, sets refresh_token as HttpOnly cookie.
     */
    @PostMapping("/token")
    fun login(
        @RequestBody request: LoginRequest,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<TokenResponse>> =
        webClient
            .post()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("grant_type", "password")
                    .with("client_id", sharedConfig.keycloak.gatewayClientId)
                    .with("client_secret", sharedConfig.keycloak.gatewayClientSecret)
                    .with("username", request.email)
                    .with("password", request.password),
            ).retrieve()
            .bodyToMono<KeycloakTokenResponse>()
            .map { keycloakResponse ->
                addRefreshTokenCookie(exchange, keycloakResponse.refreshToken, keycloakResponse.refreshExpiresIn)
                ResponseEntity.ok(
                    TokenResponse(
                        accessToken = keycloakResponse.accessToken,
                        expiresIn = keycloakResponse.expiresIn,
                        tokenType = keycloakResponse.tokenType,
                    ),
                )
            }.onErrorResume { ex ->
                log.debug("Keycloak login failed: {}", ex.message)
                Mono.just(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(),
                )
            }

    /**
     * Refresh: uses refresh_token from a cookie to get new tokens.
     */
    @PostMapping("/refresh-token")
    fun refreshToken(exchange: ServerWebExchange): Mono<ResponseEntity<TokenResponse>> {
        val refreshToken =
            extractRefreshTokenFromCookie(exchange)
                ?: return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())

        return webClient
            .post()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("grant_type", "refresh_token")
                    .with("client_id", sharedConfig.keycloak.gatewayClientId)
                    .with("client_secret", sharedConfig.keycloak.gatewayClientSecret)
                    .with("refresh_token", refreshToken),
            ).retrieve()
            .bodyToMono<KeycloakTokenResponse>()
            .map { keycloakResponse ->
                addRefreshTokenCookie(exchange, keycloakResponse.refreshToken, keycloakResponse.refreshExpiresIn)
                ResponseEntity.ok(
                    TokenResponse(
                        accessToken = keycloakResponse.accessToken,
                        expiresIn = keycloakResponse.expiresIn,
                        tokenType = keycloakResponse.tokenType,
                    ),
                )
            }.onErrorResume { ex ->
                log.debug("Keycloak token refresh failed: {}", ex.message)
                deleteRefreshTokenCookie(exchange)
                Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
            }
    }

    /**
     * Logout: invalidates the refresh_token in Keycloak and clears the cookie.
     */
    @Suppress("kotlin:S6508")
    @PostMapping("/logout")
    fun logout(exchange: ServerWebExchange): Mono<ResponseEntity<Void>> {
        val refreshToken = extractRefreshTokenFromCookie(exchange)

        deleteRefreshTokenCookie(exchange)

        if (refreshToken == null) {
            return Mono.just(ResponseEntity.noContent().build())
        }

        return logoutClient
            .post()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("client_id", sharedConfig.keycloak.gatewayClientId)
                    .with("client_secret", sharedConfig.keycloak.gatewayClientSecret)
                    .with("refresh_token", refreshToken),
            ).retrieve()
            .toBodilessEntity()
            .map { ResponseEntity.noContent().build<Void>() }
            .onErrorResume { ex ->
                log.debug("Keycloak logout failed: {}", ex.message)
                Mono.just(ResponseEntity.noContent().build())
            }
    }

    // --- Private helpers ---

    private fun keycloakTokenUrl(): String =
        "${sharedConfig.keycloak.serverUrl}/realms/${sharedConfig.keycloak.realm}/protocol/openid-connect/token"

    private fun keycloakLogoutUrl(): String =
        "${sharedConfig.keycloak.serverUrl}/realms/${sharedConfig.keycloak.realm}/protocol/openid-connect/logout"

    private fun addRefreshTokenCookie(
        exchange: ServerWebExchange,
        refreshToken: String,
        maxAgeSeconds: Long,
    ) {
        val cookie =
            ResponseCookie
                .from(sharedConfig.cookie.refreshTokenCookieName, refreshToken)
                .httpOnly(sharedConfig.cookie.httpOnly)
                .secure(sharedConfig.cookie.secure)
                .sameSite(sharedConfig.cookie.sameSite)
                .path(sharedConfig.cookie.path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build()

        exchange.response.addCookie(cookie)
    }

    private fun deleteRefreshTokenCookie(exchange: ServerWebExchange) {
        val cookie =
            ResponseCookie
                .from(sharedConfig.cookie.refreshTokenCookieName, "")
                .httpOnly(sharedConfig.cookie.httpOnly)
                .secure(sharedConfig.cookie.secure)
                .sameSite(sharedConfig.cookie.sameSite)
                .path(sharedConfig.cookie.path)
                .maxAge(Duration.ZERO)
                .build()

        exchange.response.addCookie(cookie)
    }

    private fun extractRefreshTokenFromCookie(exchange: ServerWebExchange): String? {
        val cookieName = sharedConfig.cookie.refreshTokenCookieName
        return exchange.request.cookies
            .getFirst(cookieName)
            ?.value
    }

    /**
     * Internal DTO mapping Keycloak's OAuth2 token response.
     */
    private data class KeycloakTokenResponse(
        @JsonProperty("access_token") val accessToken: String = "",
        @JsonProperty("expires_in") val expiresIn: Long = 0,
        @JsonProperty("refresh_expires_in") val refreshExpiresIn: Long = 0,
        @JsonProperty("refresh_token") val refreshToken: String = "",
        @JsonProperty("token_type") val tokenType: String = "",
    )
}
