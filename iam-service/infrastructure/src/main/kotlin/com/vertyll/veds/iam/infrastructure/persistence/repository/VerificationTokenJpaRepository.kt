package com.vertyll.veds.iam.infrastructure.persistence.repository

import com.vertyll.veds.iam.infrastructure.persistence.entity.VerificationTokenJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
internal interface VerificationTokenJpaRepository : JpaRepository<VerificationTokenJpaEntity, Long> {
    fun findByToken(token: String): Optional<VerificationTokenJpaEntity>

    fun findByUsernameAndTokenType(
        username: String,
        tokenType: String,
    ): Optional<VerificationTokenJpaEntity>

    fun findAllByUsernameAndTokenType(
        username: String,
        tokenType: String,
    ): List<VerificationTokenJpaEntity>

    fun findByAdditionalData(additionalData: String): Optional<VerificationTokenJpaEntity>
}
