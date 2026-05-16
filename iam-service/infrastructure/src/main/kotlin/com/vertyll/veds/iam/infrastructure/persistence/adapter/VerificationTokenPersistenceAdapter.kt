package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.domain.model.VerificationToken
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.VerificationTokenJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.VerificationTokenJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class VerificationTokenPersistenceAdapter(
    private val repository: VerificationTokenJpaRepository,
) : VerificationTokenRepository {
    override fun save(verificationToken: VerificationToken): VerificationToken = repository.save(verificationToken.toJpaEntity()).toDomain()

    override fun findById(id: Long): VerificationToken? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByToken(token: String): VerificationToken? = repository.findByToken(token).orElse(null)?.toDomain()

    override fun findByUsernameAndTokenType(
        username: String,
        tokenType: String,
    ): VerificationToken? = repository.findByUsernameAndTokenType(username, tokenType).orElse(null)?.toDomain()

    override fun findAllByUsernameAndTokenType(
        username: String,
        tokenType: String,
    ): List<VerificationToken> = repository.findAllByUsernameAndTokenType(username, tokenType).map { it.toDomain() }

    override fun findByAdditionalData(additionalData: String): VerificationToken? =
        repository.findByAdditionalData(additionalData).orElse(null)?.toDomain()

    override fun deleteById(id: Long) {
        repository.deleteById(id)
    }
}

private fun VerificationToken.toJpaEntity() =
    VerificationTokenJpaEntity(
        id = this.id,
        token = this.token,
        username = this.username,
        expiryDate = this.expiryDate,
        used = this.used,
        tokenType = this.tokenType,
        additionalData = this.additionalData,
        sagaId = this.sagaId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

private fun VerificationTokenJpaEntity.toDomain() =
    VerificationToken(
        id = this.id,
        token = this.token,
        username = this.username,
        expiryDate = this.expiryDate,
        used = this.used,
        tokenType = this.tokenType,
        additionalData = this.additionalData,
        sagaId = this.sagaId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
