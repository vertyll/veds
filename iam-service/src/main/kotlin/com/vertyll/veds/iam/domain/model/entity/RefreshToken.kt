package com.vertyll.veds.iam.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(
    name = "refresh_token",
    indexes = [
        jakarta.persistence.Index(name = "idx_refresh_token_token", columnList = "token"),
        jakarta.persistence.Index(name = "idx_refresh_token_username", columnList = "username"),
    ],
)
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true, length = 1024)
    var token: String,
    @Column(nullable = false)
    var username: String,
    @Column(nullable = false)
    var expiryDate: Instant,
    @Column(nullable = false)
    var revoked: Boolean = false,
    @Column(nullable = true)
    var deviceInfo: String? = null,
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
            expiryDate = Instant.now(),
            revoked = false,
            deviceInfo = null,
            version = null,
        )

    val isRevoked: Boolean get() = revoked
}
