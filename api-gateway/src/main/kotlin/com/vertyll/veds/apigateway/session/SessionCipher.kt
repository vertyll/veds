package com.vertyll.veds.apigateway.session

import org.springframework.stereotype.Component
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
internal class SessionCipher(
    properties: GatewaySessionProperties,
) {
    private companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
        private const val AES_256_KEY_BYTES = 32
    }

    private val random = SecureRandom()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    private val key: SecretKeySpec =
        run {
            val decoded =
                try {
                    decoder.decode(properties.encryptionKey)
                } catch (e: IllegalArgumentException) {
                    throw IllegalStateException("application.gateway.session.encryption-key is not valid base64", e)
                }
            check(decoded.size == AES_256_KEY_BYTES) {
                "application.gateway.session.encryption-key must decode to $AES_256_KEY_BYTES bytes " +
                    "(AES-256), got ${decoded.size}"
            }
            SecretKeySpec(decoded, ALGORITHM)
        }

    fun encrypt(plaintext: ByteArray): ByteArray {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        return nonce + cipher.doFinal(plaintext)
    }

    fun decrypt(payload: ByteArray): ByteArray {
        if (payload.size <= NONCE_BYTES) {
            throw GeneralSecurityException("stored payload is too short to contain a nonce")
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, payload, 0, NONCE_BYTES))
        return cipher.doFinal(payload, NONCE_BYTES, payload.size - NONCE_BYTES)
    }
}
