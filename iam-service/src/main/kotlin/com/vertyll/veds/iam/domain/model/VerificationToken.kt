package com.vertyll.veds.iam.domain.model

import com.vertyll.veds.iam.domain.model.TokenTypes
import java.time.Instant
import java.time.LocalDateTime

data class VerificationToken(
    val id: Long? = null,
    val token: String,
    val username: String,
    val expiryDate: LocalDateTime,
    val used: Boolean = false,
    val tokenType: String,
    val additionalData: String? = null,
    val sagaId: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    fun isTokenType(type: TokenTypes): Boolean = this.tokenType == type.value

    fun markUsed(): VerificationToken =
        copy(
            used = true,
            updatedAt = Instant.now(),
        )
}
