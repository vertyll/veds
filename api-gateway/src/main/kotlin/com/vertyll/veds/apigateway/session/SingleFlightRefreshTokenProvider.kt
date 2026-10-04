package com.vertyll.veds.apigateway.session

import org.springframework.security.oauth2.client.OAuth2AuthorizationContext
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Spring's refresh provider, run once per refresh token: concurrent requests of one session
 * share the refresh, a request still holding the old token within thirty seconds receives the
 * same result, and [SharedRefreshes] extends that to every replica.
 */
internal class SingleFlightRefreshTokenProvider(
    private val refresh: ReactiveOAuth2AuthorizedClientProvider,
    private val sharedRefreshes: SharedRefreshes,
    private val clock: Clock,
) : ReactiveOAuth2AuthorizedClientProvider {
    private val refreshes = ConcurrentHashMap<String, Refresh>()

    override fun authorize(context: OAuth2AuthorizationContext): Mono<OAuth2AuthorizedClient> {
        val current = context.authorizedClient
        val refreshToken = current?.refreshToken
        if (current == null || refreshToken == null || !expiresSoon(current.accessToken)) {
            return refresh.authorize(context)
        }
        forgetOldRefreshes()
        val created = Refresh(clock.instant(), Mono.defer { refreshShared(context, current, refreshToken) }.cache())
        val running = refreshes.putIfAbsent(refreshToken.tokenValue, created) ?: created
        return running.result.doOnError { refreshes.remove(refreshToken.tokenValue, running) }
    }

    private fun refreshShared(
        context: OAuth2AuthorizationContext,
        current: OAuth2AuthorizedClient,
        refreshToken: OAuth2RefreshToken,
    ): Mono<OAuth2AuthorizedClient> =
        sharedRefreshes
            .refresh(refreshToken.tokenValue) {
                refresh
                    .authorize(context)
                    .switchIfEmpty(Mono.error { IllegalStateException("The refresh provider declined an expired access token") })
                    .map { refreshed ->
                        val access = refreshed.accessToken
                        val issuedAt = access.issuedAt ?: clock.instant()
                        SharedRefreshes.TokenPair(
                            access.tokenValue,
                            refreshed.refreshToken?.tokenValue ?: refreshToken.tokenValue,
                            issuedAt,
                            access.expiresAt ?: issuedAt,
                        )
                    }
            }.map { tokens ->
                OAuth2AuthorizedClient(
                    current.clientRegistration,
                    current.principalName,
                    OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        tokens.accessToken,
                        tokens.issuedAt,
                        tokens.expiresAt,
                        current.accessToken.scopes,
                    ),
                    OAuth2RefreshToken(tokens.refreshToken, tokens.issuedAt),
                )
            }

    private fun expiresSoon(accessToken: OAuth2AccessToken): Boolean {
        val expiresAt = accessToken.expiresAt ?: return false
        return !clock.instant().isBefore(expiresAt.minus(CLOCK_SKEW))
    }

    private fun forgetOldRefreshes() {
        val oldest = clock.instant().minus(REUSE_WINDOW)
        refreshes.values.removeIf { it.startedAt.isBefore(oldest) }
    }

    private class Refresh(
        val startedAt: Instant,
        val result: Mono<OAuth2AuthorizedClient>,
    )

    private companion object {
        private val REUSE_WINDOW = Duration.ofSeconds(30)
        private val CLOCK_SKEW = Duration.ofSeconds(60)
    }
}
