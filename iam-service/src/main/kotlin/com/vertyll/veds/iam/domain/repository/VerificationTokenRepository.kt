package com.vertyll.veds.iam.domain.repository

import com.vertyll.veds.iam.domain.model.VerificationToken

interface VerificationTokenRepository {
    fun save(verificationToken: VerificationToken): VerificationToken

    fun findById(id: Long): VerificationToken?

    fun findByToken(token: String): VerificationToken?

    fun findByUsernameAndTokenType(
        username: String,
        tokenType: String,
    ): VerificationToken?

    fun findAllByUsernameAndTokenType(
        username: String,
        tokenType: String,
    ): List<VerificationToken>

    fun findByAdditionalData(additionalData: String): VerificationToken?

    fun deleteById(id: Long)
}
