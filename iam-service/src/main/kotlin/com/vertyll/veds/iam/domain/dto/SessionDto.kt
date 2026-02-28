package com.vertyll.veds.iam.domain.dto

import java.time.Instant

data class SessionDto(
    val id: Long? = null,
    val deviceInfo: String? = null,
    val createdAt: Instant,
    val expiryDate: Instant,
    val isCurrentSession: Boolean = false,
)
