package com.vertyll.veds.apigateway.session

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.serializer.RedisSerializer

@Configuration
internal class SessionConfig {
    @Bean
    fun springSessionDefaultRedisSerializer(cipher: SessionCipher): RedisSerializer<Any> = EncryptedSessionSerializer(cipher)
}
