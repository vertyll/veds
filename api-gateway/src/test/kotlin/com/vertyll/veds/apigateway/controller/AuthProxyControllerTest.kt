package com.vertyll.veds.apigateway.controller

import com.vertyll.veds.apigateway.config.GatewayOAuthProperties
import com.vertyll.veds.apigateway.security.AuthTransactionCookies
import com.vertyll.veds.apigateway.session.KeycloakTokenClient
import com.vertyll.veds.apigateway.session.SessionCookies
import com.vertyll.veds.apigateway.session.SessionStore
import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AuthProxyControllerTest {
    private val properties =
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

    private val controller =
        AuthProxyController(
            sharedConfig = properties,
            oauth = GatewayOAuthProperties("http://localhost:8080/auth/callback", "http://localhost:4200/"),
            authTransactionCookies = AuthTransactionCookies(properties),
            sessionCookies = mock(SessionCookies::class.java),
            sessionStore = mock(SessionStore::class.java),
            keycloakTokenClient = mock(KeycloakTokenClient::class.java),
        )

    private fun authorize(kcAction: String? = null): URI {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/authorize"))
        val response = controller.authorize(exchange, kcAction)
        assertEquals(HttpStatus.FOUND, response.statusCode)
        return assertNotNull(response.headers.location)
    }

    @Test
    fun `the browser is sent to the realm's authorization endpoint`() {
        val location = authorize()

        assertEquals("http://localhost:9000/realms/veds/protocol/openid-connect/auth", location.toString().substringBefore('?'))
    }

    @Test
    fun `the authorization request carries the parameters Keycloak needs`() {
        val query = authorize().query

        listOf("client_id=", "redirect_uri=", "response_type=code", "scope=", "state=", "code_challenge=", "code_challenge_method=S256")
            .forEach { assertTrue(query.contains(it), "missing $it in $query") }
    }

    @Test
    fun `the location header is a usable URI even though the scope contains spaces`() {
        val location = authorize()

        assertTrue(location.rawQuery.contains("scope=openid%20profile%20email"), "scope was not encoded: ${location.rawQuery}")
        assertEquals(location, URI.create(location.toString()))
    }

    @Test
    fun `the transaction cookies are issued so the callback can verify itself`() {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/authorize"))
        controller.authorize(exchange, null)

        val cookies = exchange.response.cookies
        assertNotNull(cookies.getFirst(AuthTransactionCookies.STATE_COOKIE))
        assertNotNull(cookies.getFirst(AuthTransactionCookies.VERIFIER_COOKIE))
    }

    @Test
    fun `a recognised account action is relayed to Keycloak`() {
        assertTrue(authorize("UPDATE_PASSWORD").query.contains("kc_action=UPDATE_PASSWORD"))
    }

    @Test
    fun `an unrecognised account action is dropped rather than forwarded`() {
        assertTrue(!authorize("DELETE_ACCOUNT").query.contains("kc_action"))
    }
}
