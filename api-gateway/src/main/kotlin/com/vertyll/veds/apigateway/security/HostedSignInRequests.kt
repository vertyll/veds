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
 * `GET /auth/authorize` starts the sign-in: Spring's request with PKCE, plus an allowed `kc_action`, the reader's
 * `theme`, passed on as `ui_theme`, and their `ui_locales`, so Keycloak's pages open in the theme and the language
 * FastDo shows.
 */
internal class HostedSignInRequests(
    registrations: ReactiveClientRegistrationRepository,
) : ServerOAuth2AuthorizationRequestResolver {
    companion object {
        const val AUTHORIZE_PATH = "/auth/authorize"
        private const val KC_ACTION = "kc_action"
        private val ALLOWED_ACTIONS = setOf("CONFIGURE_TOTP", "UPDATE_PASSWORD")

        /**
         * Keycloak's action removing one credential, named by its id; Keycloak asks to confirm and removes it only when
         * it is the person's own.
         */
        private val DELETE_CREDENTIAL =
            Regex("delete_credential:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        private const val UI_LOCALES = "ui_locales"
        private val ALLOWED_LANGUAGES = setOf("pl", "en")
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
        val action = query.getFirst(KC_ACTION)?.takeIf { it in ALLOWED_ACTIONS || DELETE_CREDENTIAL.matches(it) }
        val theme = query.getFirst(THEME)?.takeIf { it in ALLOWED_THEMES }
        val language = query.getFirst(UI_LOCALES)?.takeIf { it in ALLOWED_LANGUAGES }
        return delegate.resolve(exchange, clientRegistrationId).map { authorization ->
            OAuth2AuthorizationRequest
                .from(authorization)
                .additionalParameters { parameters ->
                    action?.let { parameters[KC_ACTION] = it }
                    theme?.let { parameters[UI_THEME] = it }
                    language?.let { parameters[UI_LOCALES] = it }
                }.build()
        }
    }
}
