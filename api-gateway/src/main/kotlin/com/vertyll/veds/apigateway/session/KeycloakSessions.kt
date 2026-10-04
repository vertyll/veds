package com.vertyll.veds.apigateway.session

import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
internal class KeycloakSessions(
    private val sharedConfig: SharedKeycloakProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client: WebClient by lazy { WebClient.builder().build() }

    fun revoke(refreshToken: String): Mono<Void> =
        client
            .post()
            .uri("${sharedConfig.serverUrl}/realms/${sharedConfig.realm}/protocol/openid-connect/logout")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("client_id", sharedConfig.gatewayClientId)
                    .with("client_secret", sharedConfig.gatewayClientSecret)
                    .with("refresh_token", refreshToken),
            ).retrieve()
            .toBodilessEntity()
            .then()
            .onErrorResume { e ->
                log.warn("Keycloak did not end the session: {}", e.message)
                Mono.empty()
            }
}
