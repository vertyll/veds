package com.vertyll.veds.apigateway.session

import org.slf4j.LoggerFactory
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import java.security.GeneralSecurityException

/**
 * Seals every session attribute with AES-GCM before it reaches Redis, so the tokens in a
 * session are unreadable to anyone holding a copy of the data. A value that no longer
 * decrypts — after a key rotation — reads as absent, which signs that browser out.
 */
internal class EncryptedSessionSerializer(
    private val cipher: SessionCipher,
) : RedisSerializer<Any> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val delegate = JdkSerializationRedisSerializer()

    override fun serialize(value: Any?): ByteArray = cipher.encrypt(delegate.serialize(value))

    override fun deserialize(bytes: ByteArray?): Any? {
        if (bytes == null) return null
        return try {
            delegate.deserialize(cipher.decrypt(bytes))
        } catch (e: GeneralSecurityException) {
            log.warn("Discarding a session attribute that does not decrypt: {}", e.message)
            null
        }
    }
}
