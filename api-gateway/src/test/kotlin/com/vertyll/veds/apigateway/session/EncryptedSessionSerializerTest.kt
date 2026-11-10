package com.vertyll.veds.apigateway.session

import org.junit.jupiter.api.Test
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class EncryptedSessionSerializerTest {
    private val cipher = SessionCipher(GatewaySessionProperties(Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })))
    private val serializer = EncryptedSessionSerializer(cipher)

    private val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    private val signIn =
        ClientRegistration
            .withRegistrationId("keycloak")
            .clientId("veds-api-gateway")
            .clientSecret("secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/auth/callback")
            .scope("openid", "profile", "email")
            .authorizationUri("http://localhost:9000/realms/veds/protocol/openid-connect/auth")
            .tokenUri("http://localhost:9000/realms/veds/protocol/openid-connect/token")
            .jwkSetUri("http://localhost:9000/realms/veds/protocol/openid-connect/certs")
            .issuerUri("http://localhost:9000/realms/veds")
            .userNameAttributeName("sub")
            .build()

    private val taskService =
        ClientRegistration
            .withRegistrationId("veds-task-service")
            .clientId("veds-api-gateway")
            .clientSecret("secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
            .scope("veds-task-service-audience")
            .tokenUri("http://localhost:9000/realms/veds/protocol/openid-connect/token")
            .build()

    private val idToken =
        OidcIdToken
            .withTokenValue("id-token")
            .subject("7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10")
            .claim("email", "ada@veds.local")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .build()

    private fun accessToken(value: String) = OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, value, now, now.plusSeconds(300))

    @Test
    fun `a signed-in security context survives the round trip`() {
        val user = DefaultOidcUser(listOf(OidcUserAuthority(idToken)), idToken)
        val context = SecurityContextImpl(OAuth2AuthenticationToken(user, user.authorities, "keycloak"))

        val read = assertIs<SecurityContextImpl>(serializer.deserialize(serializer.serialize(context)))

        val authentication = assertIs<OAuth2AuthenticationToken>(read.authentication)
        assertEquals("keycloak", authentication.authorizedClientRegistrationId)
        assertEquals("ada@veds.local", assertIs<DefaultOidcUser>(authentication.principal).email)
        assertEquals("id-token", (authentication.principal as DefaultOidcUser).idToken.tokenValue)
    }

    @Test
    fun `the authorized clients of a session, signed in and exchanged, survive the round trip`() {
        val clients =
            hashMapOf(
                "keycloak" to
                    OAuth2AuthorizedClient(signIn, "ada", accessToken("access-token"), OAuth2RefreshToken("refresh-token", now)),
                "veds-task-service" to OAuth2AuthorizedClient(taskService, "ada", accessToken("exchanged")),
            )

        val read = assertIs<Map<*, *>>(serializer.deserialize(serializer.serialize(clients)))

        val signedIn = assertIs<OAuth2AuthorizedClient>(read["keycloak"])
        assertEquals("access-token", signedIn.accessToken.tokenValue)
        assertEquals("refresh-token", assertNotNull(signedIn.refreshToken).tokenValue)
        val exchanged = assertIs<OAuth2AuthorizedClient>(read["veds-task-service"])
        assertEquals(AuthorizationGrantType.TOKEN_EXCHANGE, exchanged.clientRegistration.authorizationGrantType)
        assertEquals("exchanged", exchanged.accessToken.tokenValue)
    }

    @Test
    fun `the session's own fields keep their types`() {
        assertEquals(now.toEpochMilli(), serializer.deserialize(serializer.serialize(now.toEpochMilli())))
        assertEquals(604_800, serializer.deserialize(serializer.serialize(604_800)))
    }

    @Test
    fun `a value written with Java serialization reads as absent`() {
        val legacy = cipher.encrypt(JdkSerializationRedisSerializer().serialize(now.toEpochMilli()))

        assertNull(serializer.deserialize(legacy))
    }

    @Test
    fun `a value sealed with another key reads as absent`() {
        val otherCipher = SessionCipher(GatewaySessionProperties(Base64.getEncoder().encodeToString(ByteArray(32))))

        assertNull(serializer.deserialize(EncryptedSessionSerializer(otherCipher).serialize("value")))
    }
}
