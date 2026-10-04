package com.vertyll.veds.apigateway

import com.vertyll.veds.apigateway.session.KeycloakSessions
import io.netty.handler.codec.http.HttpHeaderNames
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.TokenExchangeGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoderFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import org.testcontainers.containers.GenericContainer
import reactor.core.publisher.Mono
import reactor.netty.DisposableServer
import reactor.netty.http.server.HttpServer
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class HostedSignInTest {
    @LocalServerPort
    private var port: Int = 0

    private val client: WebTestClient by lazy { WebTestClient.bindToServer().baseUrl("http://localhost:$port").build() }

    @Autowired
    private lateinit var redis: ReactiveStringRedisTemplate

    @MockitoBean
    private lateinit var accessTokens: ReactiveJwtDecoder

    @MockitoBean
    private lateinit var idTokenDecoders: ReactiveJwtDecoderFactory<ClientRegistration>

    @MockitoBean
    private lateinit var codeTokens: ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>

    @MockitoBean
    private lateinit var refreshTokens: ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest>

    @MockitoBean
    private lateinit var exchangedTokens: ReactiveOAuth2AccessTokenResponseClient<TokenExchangeGrantRequest>

    @MockitoBean
    private lateinit var keycloakSessions: KeycloakSessions

    private var refreshToken = ""

    @BeforeEach
    fun keycloakIssuesTokens() {
        received.clear()
        refreshToken = "refresh-token-${UUID.randomUUID()}"
        `when`(accessTokens.decode(ACCESS_TOKEN)).thenReturn(Mono.just(accessToken(ACCESS_TOKEN)))
        `when`(accessTokens.decode(REFRESHED_ACCESS_TOKEN)).thenReturn(Mono.just(accessToken(REFRESHED_ACCESS_TOKEN)))
        `when`(exchangedTokens.getTokenResponse(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<TokenExchangeGrantRequest>(0)
            Mono.just(tokenResponse("exchanged-for-${request.clientRegistration.registrationId}", LIFETIME_SECONDS, null))
        }
        `when`(keycloakSessions.revoke(refreshToken)).thenReturn(Mono.empty())
    }

    @Test
    fun `authorize sends the browser to Keycloak with PKCE and keeps the request in a Lax cookie`() {
        val result =
            client
                .get()
                .uri("/auth/authorize?kc_action=UPDATE_PASSWORD")
                .exchange()
                .expectStatus()
                .isFound
                .expectBody()
                .returnResult()

        val location = requireNotNull(result.responseHeaders.location).toString()
        assertTrue(location.startsWith("http://localhost:9000/realms/veds/protocol/openid-connect/auth"))
        val query = queryOf(location)
        assertEquals("veds-api-gateway", query.getFirst("client_id"))
        assertEquals("S256", query.getFirst("code_challenge_method"))
        assertEquals("UPDATE_PASSWORD", query.getFirst("kc_action"))
        assertEquals("Lax", cookie(result, AUTH_REQUEST_COOKIE)?.sameSite)
        assertNull(cookie(result, SESSION_COOKIE))
    }

    @Test
    fun `a callback with a state this browser was not given is refused`() {
        val browser = Browser()
        browser.send(client.get().uri("/auth/authorize"))

        val callback = browser.send(client.get().uri("/auth/callback?code=code&state=forged"))

        assertTrue(requireNotNull(callback.responseHeaders.location).toString().endsWith("error=state_mismatch"))
    }

    @Test
    fun `signing in gives a Strict session cookie and keeps the tokens encrypted in Redis`() {
        val browser = signedIn(LIFETIME_SECONDS)

        assertEquals("Strict", browser.cookies[SESSION_COOKIE]?.sameSite)
        browser
            .send(client.get().uri("/auth/session"))
            .also { assertEquals(HttpStatus.OK, it.status) }
            .also { assertTrue(String(requireNotNull(it.responseBodyContent)).contains(EMAIL)) }
        val keys =
            redis
                .keys("veds:session:*")
                .collectList()
                .block()
                .orEmpty()
        assertTrue(keys.isNotEmpty())
        val stored =
            keys
                .flatMap { key ->
                    redis
                        .opsForHash<String, String>()
                        .values(key)
                        .collectList()
                        .block()
                        .orEmpty()
                }.joinToString()
        assertTrue(!stored.contains(refreshToken) && !stored.contains(ACCESS_TOKEN))
    }

    @Test
    fun `each service receives a token exchanged for its own audience, once per session`() {
        val browser = signedIn(LIFETIME_SECONDS)

        browser.send(client.get().uri("/tasks/1"))
        browser.send(client.get().uri("/tasks/2"))
        browser.send(client.get().uri("/projects/1"))

        assertEquals(
            listOf(
                "/tasks/1" to "Bearer exchanged-for-veds-task-service",
                "/tasks/2" to "Bearer exchanged-for-veds-task-service",
                "/projects/1" to "Bearer exchanged-for-veds-project-service",
            ),
            received.toList(),
        )
        verify(exchangedTokens, times(2)).getTokenResponse(any())
    }

    @Test
    fun `a client with its own token gets an exchanged token and no session`() {
        val result =
            client
                .get()
                .uri("/tasks/1")
                .header("Authorization", "Bearer $ACCESS_TOKEN")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()

        assertEquals(listOf("/tasks/1" to "Bearer exchanged-for-veds-task-service"), received.toList())
        assertNull(cookie(result, SESSION_COOKIE))
    }

    @Test
    fun `a request without a session or a token reaches no service`() {
        client
            .get()
            .uri("/tasks/1")
            .exchange()
            .expectStatus()
            .isUnauthorized

        assertTrue(received.isEmpty())
    }

    @Test
    fun `an access token about to expire is refreshed before the exchange`() {
        `when`(refreshTokens.getTokenResponse(any()))
            .thenReturn(Mono.just(tokenResponse(REFRESHED_ACCESS_TOKEN, LIFETIME_SECONDS, refreshToken)))
        val browser = signedIn(EXPIRING_SECONDS)

        assertEquals(HttpStatus.OK, browser.send(client.get().uri("/tasks/1")).status)
        verify(refreshTokens).getTokenResponse(any())
    }

    @Test
    fun `a session whose refresh Keycloak refuses ends`() {
        `when`(refreshTokens.getTokenResponse(any()))
            .thenReturn(Mono.error(OAuth2AuthorizationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT))))
        val browser = signedIn(EXPIRING_SECONDS)

        assertEquals(HttpStatus.UNAUTHORIZED, browser.send(client.get().uri("/tasks/1")).status)
        assertEquals(HttpStatus.NO_CONTENT, browser.send(client.get().uri("/auth/session")).status)
        assertTrue(received.isEmpty())
    }

    @Test
    fun `logout revokes the refresh token and ends the session`() {
        val browser = signedIn(LIFETIME_SECONDS)

        assertEquals(HttpStatus.NO_CONTENT, browser.send(client.post().uri("/auth/logout")).status)
        verify(keycloakSessions).revoke(refreshToken)
        assertEquals(HttpStatus.NO_CONTENT, browser.send(client.get().uri("/auth/session")).status)
    }

    @Test
    fun `logout without a session revokes nothing`() {
        client
            .post()
            .uri("/auth/logout")
            .exchange()
            .expectStatus()
            .isNoContent

        verify(keycloakSessions, never()).revoke(anyString())
    }

    private fun signedIn(accessTokenLifetimeSeconds: Long): Browser {
        val browser = Browser()
        val location = requireNotNull(browser.send(client.get().uri("/auth/authorize")).responseHeaders.location)
        val authorization = queryOf(location.toString())
        val idToken = idToken(requireNotNull(authorization.getFirst("nonce")))
        `when`(idTokenDecoders.createDecoder(any())).thenReturn(ReactiveJwtDecoder { Mono.just(idToken) })
        `when`(codeTokens.getTokenResponse(any()))
            .thenReturn(Mono.just(tokenResponse(ACCESS_TOKEN, accessTokenLifetimeSeconds, refreshToken)))

        val callback =
            browser.send(client.get().uri("/auth/callback?code=code&state={state}", requireNotNull(authorization.getFirst("state"))))

        assertEquals(HttpStatus.FOUND, callback.status)
        assertEquals("http://localhost:4200/", callback.responseHeaders.location.toString())
        assertNotNull(browser.cookies[SESSION_COOKIE])
        return browser
    }

    private class Browser {
        val cookies = linkedMapOf<String, ResponseCookie>()

        fun send(request: WebTestClient.RequestHeadersSpec<*>): EntityExchangeResult<ByteArray> {
            cookies.values.forEach { request.cookie(it.name, it.value) }
            val result = request.exchange().expectBody().returnResult()
            result.responseCookies.values.flatten().forEach {
                if (it.maxAge.isZero) cookies.remove(it.name) else cookies[it.name] = it
            }
            return result
        }
    }

    private fun cookie(
        result: EntityExchangeResult<*>,
        name: String,
    ): ResponseCookie? = result.responseCookies.getFirst(name)

    private fun tokenResponse(
        accessToken: String,
        lifetimeSeconds: Long,
        refreshToken: String?,
    ): OAuth2AccessTokenResponse =
        OAuth2AccessTokenResponse
            .withToken(accessToken)
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .expiresIn(lifetimeSeconds)
            .scopes(setOf("openid", "profile", "email"))
            .apply { refreshToken?.let { refreshToken(it) } }
            .additionalParameters(mapOf(OidcParameterNames.ID_TOKEN to ID_TOKEN))
            .build()

    private fun idToken(nonce: String): Jwt =
        Jwt
            .withTokenValue(ID_TOKEN)
            .header("alg", "RS256")
            .issuer("http://localhost:9000/realms/veds")
            .subject(SUBJECT)
            .audience(listOf("veds-api-gateway"))
            .claim("email", EMAIL)
            .claim("nonce", nonce)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LIFETIME_SECONDS))
            .build()

    private fun accessToken(value: String): Jwt =
        Jwt
            .withTokenValue(value)
            .header("alg", "RS256")
            .subject(SUBJECT)
            .audience(listOf("veds-api-gateway"))
            .claim("email", EMAIL)
            .claim("realm_access", mapOf("roles" to listOf("USER", "offline_access")))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LIFETIME_SECONDS))
            .build()

    private fun queryOf(location: String): MultiValueMap<String, String> =
        UriComponentsBuilder
            .fromUri(URI.create(location))
            .build()
            .queryParams
            .mapValuesTo(LinkedMultiValueMap()) { (_, values) -> values.map { URLDecoder.decode(it, StandardCharsets.UTF_8) } }

    companion object {
        private const val ACCESS_TOKEN = "access-token"
        private const val REFRESHED_ACCESS_TOKEN = "refreshed-access-token"
        private const val ID_TOKEN = "id-token"
        private const val SUBJECT = "7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10"
        private const val EMAIL = "ada@veds.local"
        private const val SESSION_COOKIE = "VEDS_SESSION"
        private const val AUTH_REQUEST_COOKIE = "KEYCLOAK_AUTH_REQUEST"
        private const val LIFETIME_SECONDS = 300L
        private const val EXPIRING_SECONDS = 5L

        private val received = ConcurrentLinkedQueue<Pair<String, String?>>()
        private val redisContainer = GenericContainer("redis:8-alpine").withExposedPorts(6379).apply { start() }
        private val service: DisposableServer =
            HttpServer
                .create()
                .port(0)
                .handle { request, response ->
                    received.add(request.uri() to request.requestHeaders().get(HttpHeaderNames.AUTHORIZATION))
                    response.sendString(Mono.just("{}"))
                }.bindNow()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }
            listOf("IAM", "MAIL", "PROJECT", "TASK", "TRANSLATION", "FILE", "NOTIFICATION").forEach { name ->
                registry.add("${name}_SERVICE_URL") { "http://localhost:${service.port()}" }
            }
        }

        @JvmStatic
        @AfterAll
        fun stop() {
            service.disposeNow()
            redisContainer.stop()
        }
    }
}
