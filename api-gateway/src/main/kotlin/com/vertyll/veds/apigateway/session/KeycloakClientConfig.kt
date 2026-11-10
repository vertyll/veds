package com.vertyll.veds.apigateway.session

import com.vertyll.veds.apigateway.config.GatewayOAuthProperties
import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive.JwkSetUriReactiveJwtDecoderBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.security.oauth2.client.DelegatingReactiveOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.RefreshOidcUserReactiveOAuth2AuthorizationSuccessHandler
import org.springframework.security.oauth2.client.RefreshTokenReactiveOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.TokenExchangeReactiveOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.TokenExchangeGrantRequest
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveRefreshTokenTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveTokenExchangeTokenResponseClient
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator
import org.springframework.security.oauth2.client.oidc.authentication.ReactiveOidcIdTokenDecoderFactory
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoderFactory
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Clock

/**
 * The gateway's OAuth2 clients: [SIGN_IN] signs the browser in, and one token-exchange
 * registration per service turns the person's token into one whose only audience is that
 * service, so a token leaked from one service opens no other.
 *
 * Every call to Keycloak, for tokens, for its signing keys or to end a session, goes through
 * [keycloakWebClient], built on Boot's connector and so bounded by the connect and read timeouts
 * under `spring.http.clients`: a Keycloak that stops answering fails a request instead of
 * holding it open.
 */
@Configuration
internal class KeycloakClientConfig {
    @Bean
    fun keycloakWebClient(connector: ClientHttpConnector): WebClient = WebClient.builder().clientConnector(connector).build()

    /** The keys that verify a bearer token are fetched with the same bounded client. */
    @Bean
    fun keycloakJwkSetClient(keycloakWebClient: WebClient): JwkSetUriReactiveJwtDecoderBuilderCustomizer =
        JwkSetUriReactiveJwtDecoderBuilderCustomizer { builder -> builder.webClient(keycloakWebClient) }

    @Bean
    fun clientRegistrationRepository(
        sharedConfig: SharedKeycloakProperties,
        oauth: GatewayOAuthProperties,
    ): ReactiveClientRegistrationRepository {
        val realm = "${sharedConfig.serverUrl}/realms/${sharedConfig.realm}"
        val endpoints = "$realm/protocol/openid-connect"
        val signIn =
            ClientRegistration
                .withRegistrationId(SIGN_IN)
                .clientName("Keycloak")
                .clientId(sharedConfig.gatewayClientId)
                .clientSecret(sharedConfig.gatewayClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(oauth.redirectUri)
                .scope(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL)
                .authorizationUri("$endpoints/auth")
                .tokenUri("$endpoints/token")
                .jwkSetUri("$endpoints/certs")
                .issuerUri(realm)
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .build()
        val services =
            oauth.serviceAudiences.map { audience ->
                ClientRegistration
                    .withRegistrationId(audience)
                    .clientId(sharedConfig.gatewayClientId)
                    .clientSecret(sharedConfig.gatewayClientSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
                    .scope(audience + AUDIENCE_SCOPE_SUFFIX)
                    .tokenUri("$endpoints/token")
                    .build()
            }
        return InMemoryReactiveClientRegistrationRepository(listOf(signIn) + services)
    }

    private fun callerAccessToken(principal: Any?): Mono<OAuth2Token> {
        val token = (principal as? AbstractOAuth2TokenAuthenticationToken<*>)?.token ?: return Mono.empty()
        return Mono.just(OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token.tokenValue, token.issuedAt, token.expiresAt))
    }

    @Bean
    fun authorizedClientRepository(): ServerOAuth2AuthorizedClientRepository = StartedSessionAuthorizedClientRepository()

    /**
     * Verifies the ID token at sign-in as [ReactiveOidcIdTokenDecoderFactory] does, RS256 with the
     * default and OIDC validators and claim types, but fetches the keys with [keycloakWebClient],
     * which that factory offers no way to set.
     */
    @Bean
    fun idTokenDecoderFactory(keycloakWebClient: WebClient): ReactiveJwtDecoderFactory<ClientRegistration> =
        ReactiveJwtDecoderFactory { registration ->
            NimbusReactiveJwtDecoder
                .withJwkSetUri(
                    requireNotNull(registration.providerDetails.jwkSetUri) { "${registration.registrationId} names no JWK Set URI" },
                ).jwsAlgorithm(SignatureAlgorithm.RS256)
                .webClient(keycloakWebClient)
                .build()
                .apply {
                    setJwtValidator(JwtValidators.createDefaultWithValidators(OidcIdTokenValidator(registration)))
                    setClaimSetConverter(ClaimTypeConverter(ReactiveOidcIdTokenDecoderFactory.createDefaultClaimTypeConverters()))
                }
        }

    @Bean
    fun authorizationCodeTokenResponseClient(
        keycloakWebClient: WebClient,
    ): ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> =
        WebClientReactiveAuthorizationCodeTokenResponseClient().apply { setWebClient(keycloakWebClient) }

    @Bean
    fun refreshTokenResponseClient(keycloakWebClient: WebClient): ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> =
        WebClientReactiveRefreshTokenTokenResponseClient().apply { setWebClient(keycloakWebClient) }

    @Bean
    fun tokenExchangeResponseClient(keycloakWebClient: WebClient): ReactiveOAuth2AccessTokenResponseClient<TokenExchangeGrantRequest> =
        WebClientReactiveTokenExchangeTokenResponseClient().apply {
            setWebClient(keycloakWebClient)
            addParametersConverter { request ->
                LinkedMultiValueMap(mapOf("audience" to listOf(request.clientRegistration.registrationId)))
            }
        }

    @Bean
    fun authorizedClientManager(
        clientRegistrations: ReactiveClientRegistrationRepository,
        authorizedClients: ServerOAuth2AuthorizedClientRepository,
        refreshTokenResponseClient: ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest>,
        tokenExchangeResponseClient: ReactiveOAuth2AccessTokenResponseClient<TokenExchangeGrantRequest>,
        idTokenDecoderFactory: ReactiveJwtDecoderFactory<ClientRegistration>,
        sharedRefreshes: SharedRefreshes,
    ): ReactiveOAuth2AuthorizedClientManager {
        val refreshedUser =
            RefreshOidcUserReactiveOAuth2AuthorizationSuccessHandler().apply {
                setJwtDecoderFactory(idTokenDecoderFactory)
            }
        val refresh =
            RefreshTokenReactiveOAuth2AuthorizedClientProvider().apply {
                setAccessTokenResponseClient(refreshTokenResponseClient)
                setAuthorizationSuccessHandler(refreshedUser)
            }
        val tokenExchange =
            TokenExchangeReactiveOAuth2AuthorizedClientProvider().apply {
                setAccessTokenResponseClient(tokenExchangeResponseClient)
                setSubjectTokenResolver { context -> callerAccessToken(context.getPrincipal()) }
            }
        val clientProvider =
            DelegatingReactiveOAuth2AuthorizedClientProvider(
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().build(),
                SingleFlightRefreshTokenProvider(refresh, sharedRefreshes, Clock.systemUTC()),
                tokenExchange,
            )
        return DefaultReactiveOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients).apply {
            setAuthorizedClientProvider(clientProvider)
        }
    }

    companion object {
        const val SIGN_IN = "keycloak"
        private const val AUDIENCE_SCOPE_SUFFIX = "-audience"
    }
}
