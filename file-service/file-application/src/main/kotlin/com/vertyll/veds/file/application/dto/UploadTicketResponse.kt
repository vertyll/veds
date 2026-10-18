package com.vertyll.veds.file.application.dto

import java.time.Instant
import java.util.UUID

data class UploadTicketResponse(
    val fileId: UUID,
    val uploadUrl: String,
    val expiresAt: Instant,
    val maxSizeBytes: Long,
)
