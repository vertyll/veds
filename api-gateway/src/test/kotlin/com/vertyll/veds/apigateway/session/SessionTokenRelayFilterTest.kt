package com.vertyll.veds.apigateway.session

import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Instant
import kotlin.test.assertEquals

internal class SessionTokenRelayFilterTest {
    private val sharedConfig =
        SharedKeycloakProperties(
            serverUrl = "http://localhost:9000",
            realm = "veds",
            adminClientId = "veds-service-account",
            adminClientSecret = "secret",
            gatewayClientId = "veds-api-gateway",
            gatewayClientSecret = "secret",
            rolesClaimPath = "realm_access.roles",
            cookie =
                SharedKeycloakProperties.CookieProperties(
                    refreshTokenCookieName = "KEYCLOAK_REFRESH_TOKEN",
                    httpOnly = true,
                    secure = false,
                    sameSite = "Strict",
                    path = "/",
                ),
        )

    private class RecordingChain : WebFilterChain {
        val authorizationHeaders = mutableListOf<String?>()

        override fun filter(exchange: ServerWebExchange): Mono<Void> {
            authorizationHeaders += exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            return Mono.empty()
        }
    }

    private fun filterWith(
        store: SessionStore,
        tokenClient: KeycloakTokenClient = mock(KeycloakTokenClient::class.java),
    ) = SessionTokenRelayFilter(
        sessionStore = store,
        sessionCookies = SessionCookies(sharedConfig),
        keycloakTokenClient = tokenClient,
    )

    private fun exchangeWithSession() =
        MockServerWebExchange.from(
            MockServerHttpRequest
                .get("/notifications")
                .cookie(org.springframework.http.HttpCookie(SessionCookies.SESSION_COOKIE, "session-id")),
        )

    private fun session() =
        AuthSession(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            accessTokenExpiresAt = Instant.now().plusSeconds(300),
            subject = "subject",
            email = "user@example.com",
            roles = listOf("USER"),
        )

    @Test
    fun `a resolved session relays the bearer token and runs the chain exactly once`() {
        val store =
            object : NoOpSessionStore() {
                override fun find(sessionId: String): Mono<AuthSession> = Mono.just(session())
            }
        val chain = RecordingChain()

        filterWith(store).filter(exchangeWithSession(), chain).block()

        assertEquals(listOf<String?>("Bearer access-token"), chain.authorizationHeaders)
    }

    @Test
    fun `an unknown session runs the chain exactly once without a bearer token`() {
        val chain = RecordingChain()

        filterWith(NoOpSessionStore()).filter(exchangeWithSession(), chain).block()

        assertEquals(listOf<String?>(null), chain.authorizationHeaders)
    }

    @Test
    fun `a refresh that lost the race uses the session the winner stored instead of dropping it`() {
        val stale = session().copy(accessTokenExpiresAt = Instant.now().minusSeconds(1))
        val refreshedByWinner = session().copy(accessToken = "winner-token", refreshToken = "winner-refresh")
        var deletions = 0
        val store =
            object : NoOpSessionStore() {
                private var reads = 0

                override fun find(sessionId: String): Mono<AuthSession> {
                    reads++
                    return Mono.just(if (reads == 1) stale else refreshedByWinner)
                }

                override fun delete(sessionId: String): Mono<Void> {
                    deletions++
                    return Mono.empty()
                }
            }
        val rotatedAway =
            mock(KeycloakTokenClient::class.java).also {
                `when`(it.refresh(stale.refreshToken)).thenReturn(Mono.error(IllegalStateException("400 Bad Request")))
            }
        val chain = RecordingChain()

        filterWith(store, rotatedAway).filter(exchangeWithSession(), chain).block()

        assertEquals(listOf<String?>("Bearer winner-token"), chain.authorizationHeaders)
        assertEquals(0, deletions, "a session another request has just refreshed must never be deleted")
    }

    @Test
    fun `a refresh that nobody else won drops the session`() {
        val stale = session().copy(accessTokenExpiresAt = Instant.now().minusSeconds(1))
        var deletions = 0
        val store =
            object : NoOpSessionStore() {
                override fun find(sessionId: String): Mono<AuthSession> = Mono.just(stale)

                override fun delete(sessionId: String): Mono<Void> {
                    deletions++
                    return Mono.empty()
                }
            }
        val dead =
            mock(KeycloakTokenClient::class.java).also {
                `when`(it.refresh(stale.refreshToken)).thenReturn(Mono.error(IllegalStateException("400 Bad Request")))
            }
        val chain = RecordingChain()

        filterWith(store, dead).filter(exchangeWithSession(), chain).block()

        assertEquals(listOf<String?>(null), chain.authorizationHeaders)
        assertEquals(1, deletions)
    }

    @Test
    fun `a request that does not win the claim never calls Keycloak and waits for the new tokens`() {
        val stale = session().copy(accessTokenExpiresAt = Instant.now().minusSeconds(1))
        val refreshedByWinner = session().copy(accessToken = "winner-token", refreshToken = "winner-refresh")
        val store =
            object : NoOpSessionStore() {
                private var reads = 0

                override fun find(sessionId: String): Mono<AuthSession> {
                    reads++
                    return Mono.just(if (reads < 3) stale else refreshedByWinner)
                }

                override fun claimRefresh(
                    sessionId: String,
                    ttl: java.time.Duration,
                ): Mono<Boolean> = Mono.just(false)
            }
        val keycloak = mock(KeycloakTokenClient::class.java)
        val chain = RecordingChain()

        filterWith(store, keycloak).filter(exchangeWithSession(), chain).block()

        assertEquals(listOf<String?>("Bearer winner-token"), chain.authorizationHeaders)
        verify(keycloak, never()).refresh(anyString())
    }

    private open class NoOpSessionStore : SessionStore {
        override fun create(session: AuthSession): Mono<String> = Mono.empty()

        override fun find(sessionId: String): Mono<AuthSession> = Mono.empty()

        override fun update(
            sessionId: String,
            session: AuthSession,
        ): Mono<Void> = Mono.empty()

        override fun delete(sessionId: String): Mono<Void> = Mono.empty()

        override fun claimRefresh(
            sessionId: String,
            ttl: java.time.Duration,
        ): Mono<Boolean> = Mono.just(true)
    }
}
