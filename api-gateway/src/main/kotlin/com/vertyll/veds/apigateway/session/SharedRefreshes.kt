package com.vertyll.veds.apigateway.session

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.HexFormat

/**
 * One refresh per refresh token across replicas. The replica that claims
 * `<prefix>:refresh-lock:<sha256>` calls Keycloak and leaves the new tokens under
 * `<prefix>:refresh-result:<sha256>` for thirty seconds; the others take them from there
 * instead of presenting a rotated token twice. Without Redis a replica refreshes on its own.
 */
@Component
internal class SharedRefreshes(
    private val redis: ReactiveStringRedisTemplate?,
    private val redisKeyProperties: RedisKeyProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun refresh(
        refreshToken: String,
        keycloak: () -> Mono<TokenPair>,
    ): Mono<TokenPair> {
        val store = redis ?: return Mono.defer(keycloak)
        val id = sha256(refreshToken)
        val lockKey = "${redisKeyProperties.keyPrefix}:refresh-lock:$id"
        val resultKey = "${redisKeyProperties.keyPrefix}:refresh-result:$id"
        return read(store, resultKey)
            .map<Claim> { Claim.Done(it) }
            .switchIfEmpty(
                Mono.defer {
                    store
                        .opsForValue()
                        .setIfAbsent(lockKey, "1", LOCK_TTL)
                        .map { claimed -> if (claimed) Claim.Leader else Claim.Follower }
                },
            ).onErrorResume { e ->
                log.warn("Redis unavailable, refreshing without coordinating replicas: {}", e.message)
                Mono.just(Claim.Unavailable)
            }.flatMap { claim ->
                when (claim) {
                    is Claim.Done -> Mono.just(claim.pair)
                    Claim.Leader -> lead(store, lockKey, resultKey, keycloak)
                    Claim.Follower -> awaitOtherReplica(store, lockKey, resultKey).switchIfEmpty(Mono.defer(keycloak))
                    Claim.Unavailable -> Mono.defer(keycloak)
                }
            }
    }

    private fun lead(
        store: ReactiveStringRedisTemplate,
        lockKey: String,
        resultKey: String,
        keycloak: () -> Mono<TokenPair>,
    ): Mono<TokenPair> =
        Mono
            .defer(keycloak)
            .onErrorResume { refused ->
                store
                    .delete(lockKey)
                    .onErrorResume { Mono.empty() }
                    .then(Mono.error(refused))
            }.flatMap { pair ->
                store
                    .opsForValue()
                    .set(resultKey, pair.serialize(), RESULT_TTL)
                    .onErrorResume { e ->
                        log.warn("Could not share the refreshed tokens with other replicas: {}", e.message)
                        Mono.just(false)
                    }.thenReturn(pair)
            }

    private fun awaitOtherReplica(
        store: ReactiveStringRedisTemplate,
        lockKey: String,
        resultKey: String,
    ): Mono<TokenPair> =
        Mono
            .defer {
                read(store, resultKey)
                    .map<Wait> { Wait.Found(it) }
                    .switchIfEmpty(store.hasKey(lockKey).flatMap { held -> if (held) Mono.empty() else Mono.just(Wait.Abandoned) })
            }.repeatWhenEmpty(WAIT_ATTEMPTS) { attempts -> attempts.delayElements(WAIT_INTERVAL) }
            .onErrorResume { e ->
                log.warn("Redis unavailable while waiting for another replica's refresh: {}", e.message)
                Mono.empty()
            }.flatMap { wait -> if (wait is Wait.Found) Mono.just(wait.pair) else Mono.empty() }

    private fun read(
        store: ReactiveStringRedisTemplate,
        resultKey: String,
    ): Mono<TokenPair> = store.opsForValue().get(resultKey).mapNotNull { TokenPair.parse(it) }

    private sealed interface Claim {
        data class Done(
            val pair: TokenPair,
        ) : Claim

        data object Leader : Claim

        data object Follower : Claim

        data object Unavailable : Claim
    }

    private sealed interface Wait {
        data class Found(
            val pair: TokenPair,
        ) : Wait

        data object Abandoned : Wait
    }

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val issuedAt: Instant,
        val expiresAt: Instant,
    ) {
        fun serialize(): String =
            listOf(accessToken, refreshToken, issuedAt.toEpochMilli().toString(), expiresAt.toEpochMilli().toString())
                .joinToString(SEPARATOR)

        override fun toString(): String = "TokenPair(accessToken=***, refreshToken=***, issuedAt=$issuedAt, expiresAt=$expiresAt)"

        companion object {
            private const val FIELDS = 4

            fun parse(value: String): TokenPair? {
                val fields = value.split(SEPARATOR)
                if (fields.size != FIELDS) return null
                return TokenPair(
                    fields[0],
                    fields[1],
                    Instant.ofEpochMilli(fields[2].toLong()),
                    Instant.ofEpochMilli(fields.last().toLong()),
                )
            }
        }
    }

    companion object {
        private val LOCK_TTL = Duration.ofSeconds(10)
        private val RESULT_TTL = Duration.ofSeconds(30)
        private val WAIT_INTERVAL = Duration.ofMillis(100)
        private const val WAIT_ATTEMPTS = 50
        private const val SEPARATOR = "\n"

        fun inProcessOnly(): SharedRefreshes = SharedRefreshes(null, RedisKeyProperties(""))

        private fun sha256(value: String): String =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)))
    }
}
