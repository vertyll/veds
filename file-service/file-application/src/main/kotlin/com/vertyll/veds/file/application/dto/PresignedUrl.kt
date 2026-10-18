package com.vertyll.veds.file.application.dto

import java.time.Instant

data class PresignedUrl(
    val url: String,
    val expiresAt: Instant,
)
