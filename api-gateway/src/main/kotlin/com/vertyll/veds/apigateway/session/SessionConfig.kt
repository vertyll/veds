package com.vertyll.veds.apigateway.session

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.session.config.ReactiveSessionRepositoryCustomizer
import org.springframework.session.data.redis.ReactiveRedisSessionRepository
import org.springframework.session.data.redis.RedisSessionMapper
import reactor.core.publisher.Mono

@Configuration
internal class SessionConfig {
    @Bean
    fun springSessionDefaultRedisSerializer(cipher: SessionCipher): RedisSerializer<Any> = EncryptedSessionSerializer(cipher)

    /**
     * A session whose own fields, such as its creation time, read as absent, because they no longer
     * decrypt or were written in an older format, is treated as no session: it is deleted and the
     * browser starts again signed out, instead of every request failing on it.
     */
    @Bean
    fun unreadableSessionsAreDiscarded(): ReactiveSessionRepositoryCustomizer<ReactiveRedisSessionRepository> =
        ReactiveSessionRepositoryCustomizer { repository ->
            val mapper = RedisSessionMapper()
            repository.setRedisSessionMapper { id, entries ->
                Mono
                    .fromCallable { mapper.apply(id, entries) }
                    .onErrorResume(IllegalStateException::class.java) { Mono.empty() }
            }
        }
}
