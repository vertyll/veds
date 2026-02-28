package com.vertyll.veds.identity.domain.model.entity

import com.vertyll.veds.identity.domain.model.enums.TokenTypes
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(
    name = "verification_token",
    indexes = [
        jakarta.persistence.Index(name = "idx_verification_token_token", columnList = "token"),
        jakarta.persistence.Index(name = "idx_verification_token_username", columnList = "username"),
    ],
)
class VerificationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true, length = 1024)
    var token: String,
    @Column(nullable = false)
    var username: String,
    @Column(nullable = false)
    var expiryDate: LocalDateTime,
    @Column(nullable = false)
    var used: Boolean = false,
    @Column(nullable = false)
    var tokenType: String,
    @Column(nullable = true)
    var additionalData: String? = null,
    @Column(nullable = true)
    var sagaId: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    val version: Long? = null,
) {
    constructor() :
        this(
            id = null,
            token = "",
            username = "",
            expiryDate = LocalDateTime.now(),
            used = false,
            tokenType = "",
            additionalData = null,
            sagaId = null,
            version = null,
        )

    fun isTokenType(type: TokenTypes): Boolean = this.tokenType == type.value
}
