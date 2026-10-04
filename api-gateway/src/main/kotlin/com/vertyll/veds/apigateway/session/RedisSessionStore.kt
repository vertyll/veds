package com.vertyll.veds.apigateway.session

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

@Component
internal class RedisSessionStore(
    private val redis: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val sessionCipher: SessionCipher,
    private val properties: GatewaySessionProperties,
    private val redisKeys: RedisKeyProperties,
) : SessionStore {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val SESSION_ID_BYTES = 32
    }

    private val random = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    override fun create(session: AuthSession): Mono<String> {
        val sessionId = encoder.encodeToString(ByteArray(SESSION_ID_BYTES).also(random::nextBytes))
        return redis
            .opsForValue()
            .set(key(sessionId), seal(session), properties.ttl)
            .thenReturn(sessionId)
    }

    override fun find(sessionId: String): Mono<AuthSession> =
        redis
            .opsForValue()
            .get(key(sessionId))
            .flatMap { payload ->
                try {
                    Mono.just(open(payload))
                } catch (e: GeneralSecurityException) {
                    logger.warn("Discarding undecryptable session record: {}", e.message)
                    delete(sessionId).then(Mono.empty())
                } catch (e: JacksonException) {
                    logger.warn("Discarding unparseable session record: {}", e.message)
                    delete(sessionId).then(Mono.empty())
                }
            }

    override fun update(
        sessionId: String,
        session: AuthSession,
    ): Mono<Void> =
        redis
            .opsForValue()
            .set(key(sessionId), seal(session), properties.ttl)
            .then()

    override fun delete(sessionId: String): Mono<Void> = redis.delete(key(sessionId)).then()

    override fun claimRefresh(
        sessionId: String,
        ttl: Duration,
    ): Mono<Boolean> = redis.opsForValue().setIfAbsent("${redisKeys.keyPrefix}:refresh-lock:$sessionId", "1", ttl)

    private fun seal(session: AuthSession): String = sessionCipher.encrypt(objectMapper.writeValueAsString(session))

    private fun open(payload: String): AuthSession = objectMapper.readValue(sessionCipher.decrypt(payload), AuthSession::class.java)

    private fun key(sessionId: String) = "${redisKeys.keyPrefix}:session:$sessionId"
}
