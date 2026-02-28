package com.vertyll.projecta.identity.domain.repository

import com.vertyll.projecta.identity.domain.model.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>

    fun findByUsername(username: String): List<RefreshToken>

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.username = :username")
    fun revokeAllUserTokens(username: String)

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    fun deleteAllExpiredTokens(now: Instant)
}
