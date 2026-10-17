package com.vertyll.veds.apigateway.security

import com.vertyll.veds.apigateway.session.KeycloakClientConfig
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * `GET /auth/authorize` starts the sign-in: Spring's request with PKCE, plus an allowed `kc_action`, and the reader's
 * `theme`, passed on as `ui_theme` so Keycloak's pages open in the theme FastDo shows.
 */
internal class HostedSignInRequests(
    registrations: ReactiveClientRegistrationRepository,
) : ServerOAuth2AuthorizationRequestResolver {
    companion object {
        const val AUTHORIZE_PATH = "/auth/authorize"
        private const val KC_ACTION = "kc_action"
        private val ALLOWED_ACTIONS = setOf("CONFIGURE_TOTP", "UPDATE_PASSWORD", "delete_credential")
        private const val THEME = "theme"
        private const val UI_THEME = "ui_theme"
        private val ALLOWED_THEMES = setOf("light", "dark")
    }

    private val delegate =
        DefaultServerOAuth2AuthorizationRequestResolver(registrations).apply {
            setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce())
        }

    override fun resolve(exchange: ServerWebExchange): Mono<OAuth2AuthorizationRequest> =
        if (exchange.request.path
                .pathWithinApplication()
                .value() == AUTHORIZE_PATH
        ) {
            resolve(exchange, KeycloakClientConfig.SIGN_IN)
        } else {
            Mono.empty()
        }

    override fun resolve(
        exchange: ServerWebExchange,
        clientRegistrationId: String,
    ): Mono<OAuth2AuthorizationRequest> {
        val query = exchange.request.queryParams
        val action = query.getFirst(KC_ACTION)?.takeIf { it in ALLOWED_ACTIONS }
        val theme = query.getFirst(THEME)?.takeIf { it in ALLOWED_THEMES }
        return delegate.resolve(exchange, clientRegistrationId).map { authorization ->
            OAuth2AuthorizationRequest
                .from(authorization)
                .additionalParameters { parameters ->
                    action?.let { parameters[KC_ACTION] = it }
                    theme?.let { parameters[UI_THEME] = it }
                }.build()
        }
    }
}
