package com.vertyll.veds.apigateway.session

import org.slf4j.LoggerFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.security.jackson.SecurityJacksonModules
import tools.jackson.databind.json.JsonMapper
import java.security.GeneralSecurityException

/**
 * Writes every session attribute as JSON and seals it with AES-GCM before it reaches Redis, so
 * the tokens in a session are unreadable to anyone holding a copy of the data.
 *
 * JSON rather than Java serialization: reading Java-serialized bytes runs code the bytes choose,
 * and ties every value to a class version, so a rolling deploy can break reads. The mapper carries
 * Spring Security's Jackson modules, which know the security context and the OAuth2 types and
 * allow only those types to be read back.
 *
 * A value that no longer decrypts, after a key rotation, or that is not such JSON, such as one
 * written by the earlier Java serialization, reads as absent; [SessionConfig] then discards the
 * session, which signs that browser out once.
 */
internal class EncryptedSessionSerializer(
    private val cipher: SessionCipher,
) : RedisSerializer<Any> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val delegate =
        GenericJacksonJsonRedisSerializer(
            JsonMapper
                .builder()
                .addModules(SecurityJacksonModules.getModules(javaClass.classLoader))
                .build(),
        )

    override fun serialize(value: Any?): ByteArray = cipher.encrypt(delegate.serialize(value))

    override fun deserialize(bytes: ByteArray?): Any? {
        if (bytes == null) return null
        return try {
            delegate.deserialize(cipher.decrypt(bytes))
        } catch (e: GeneralSecurityException) {
            log.warn("Discarding a session attribute that does not decrypt: {}", e.message)
            null
        } catch (e: SerializationException) {
            log.warn("Discarding a session attribute that is not readable JSON: {}", e.message)
            null
        }
    }
}
